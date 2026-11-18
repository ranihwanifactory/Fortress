const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const CHARLIE_UID = "charlie_uid_303";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfilePayload(uid, nickname, ts) {
  return {
    userId: uid,
    nickname,
    listedInRanking: true,
    level: 1,
    xp: 0,
    totalXp: 0,
    wins: 0,
    losses: 0,
    onlineWins: 0,
    onlineLosses: 0,
    rankPoints: 1000,
    winStreak: 0,
    bestWinStreak: 0,
    totalDamage: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validRoomPayload(hostUid, ts) {
  return {
    roomCode: "K7M2P9",
    roomTitle: "초원 한판 붙자!",
    hostUid,
    hostName: "블루캡틴",
    hostLevel: 3,
    guestUid: "",
    guestName: "",
    guestLevel: 0,
    guestReady: false,
    participantUids: [hostUid],
    mapTheme: "GRASSLAND",
    terrainSeed: 123456789,
    status: "WAITING",
    activeTurnUid: hostUid,
    turnSeq: 0,
    windForce: 2,
    lastActionType: "NONE",
    lastActionUid: "",
    lastTankX: 320,
    lastTankY: 500,
    lastFacingRight: true,
    lastAimAngle: 45,
    lastPower: 0.5,
    lastWeapon: "STANDARD",
    lastUsedItem: "",
    winnerUid: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(
    unauthDb.collection("player_profiles").where("listedInRanking", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("player_profiles").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, "Alice", now))
  );
  await assertFails(
    unauthDb.collection("match_rooms").where("status", "==", "WAITING").get()
  );
  await assertFails(
    unauthDb.collection("room_invites").where("toUid", "==", ALICE_UID).get()
  );
});

test("PlayerProfile: owner create/update/level-up & leaderboard query succeed; cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const updated = new Date(Date.now() - 1000);

  // Create own profile
  await assertSucceeds(
    aliceDb.collection("player_profiles").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, "블루캡틴", created))
  );

  // Reject ghost field ("id") on create
  await assertFails(
    bobDb.collection("player_profiles").doc(BOB_UID).set({
      ...validProfilePayload(BOB_UID, "레드불독", created),
      id: BOB_UID,
    })
  );

  // Reject future timestamp
  await assertFails(
    bobDb.collection("player_profiles").doc(BOB_UID).set(
      validProfilePayload(BOB_UID, "레드불독", new Date(Date.now() + 60000))
    )
  );

  // Update nickname
  await assertSucceeds(
    aliceDb.collection("player_profiles").doc(ALICE_UID).update({
      nickname: "에이스전차",
      updatedAt: updated,
    })
  );

  // Record match progression & level up
  await assertSucceeds(
    aliceDb.collection("player_profiles").doc(ALICE_UID).update({
      level: 2,
      xp: 20,
      totalXp: 120,
      wins: 1,
      losses: 0,
      onlineWins: 1,
      onlineLosses: 0,
      rankPoints: 1035,
      winStreak: 1,
      bestWinStreak: 1,
      totalDamage: 115,
      updatedAt: updated,
    })
  );

  // Reject modifying createdAt
  await assertFails(
    aliceDb.collection("player_profiles").doc(ALICE_UID).update({
      nickname: "변조시도",
      createdAt: updated,
      updatedAt: updated,
    })
  );

  // Cross-user write rejected
  await assertFails(
    bobDb.collection("player_profiles").doc(ALICE_UID).update({
      nickname: "해킹",
      updatedAt: updated,
    })
  );

  // Filtered leaderboard query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("player_profiles").where("listedInRanking", "==", true).get()
  );
  await assertFails(bobDb.collection("player_profiles").get());
});

test("MatchRoom: full multiplayer lifecycle (create, list lobby, join, ready, start, turn sync, finish, lock)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const t1 = new Date(Date.now() - 5000);
  const t2 = new Date(Date.now() - 4000);
  const t3 = new Date(Date.now() - 3000);
  const t4 = new Date(Date.now() - 2000);
  const t5 = new Date(Date.now() - 1000);

  // 1. Host creates WAITING room
  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_1").set(validRoomPayload(ALICE_UID, created))
  );

  // 2. Lobby query filtered by status == WAITING succeeds; unfiltered fails
  await assertSucceeds(
    bobDb.collection("match_rooms").where("status", "==", "WAITING").get()
  );
  await assertFails(bobDb.collection("match_rooms").get());

  // 3. Guest joins room
  await assertSucceeds(
    bobDb.collection("match_rooms").doc("room_1").update({
      guestUid: BOB_UID,
      guestName: "레드불독",
      guestLevel: 2,
      guestReady: false,
      participantUids: [ALICE_UID, BOB_UID],
      updatedAt: t1,
    })
  );

  // 4. Guest toggles ready
  await assertSucceeds(
    bobDb.collection("match_rooms").doc("room_1").update({
      guestReady: true,
      updatedAt: t2,
    })
  );

  // 5. Host starts match
  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_1").update({
      status: "PLAYING",
      activeTurnUid: ALICE_UID,
      terrainSeed: 987654,
      windForce: -3,
      updatedAt: t3,
    })
  );

  // Outsider (Charlie) cannot read PLAYING room
  await assertFails(charlieDb.collection("match_rooms").doc("room_1").get());

  // Non-active player (Bob) cannot submit turn action while it is Alice's turn
  await assertFails(
    bobDb.collection("match_rooms").doc("room_1").update({
      activeTurnUid: ALICE_UID,
      turnSeq: 1,
      windForce: 1,
      lastActionType: "FIRE",
      lastActionUid: BOB_UID,
      lastTankX: 1820,
      lastTankY: 480,
      lastFacingRight: false,
      lastAimAngle: 52,
      lastPower: 0.72,
      lastWeapon: "WATER",
      lastUsedItem: "",
      updatedAt: t4,
    })
  );

  // 6. Active player (Alice) submits turn 1 shot
  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_1").update({
      activeTurnUid: BOB_UID,
      turnSeq: 1,
      windForce: 4,
      lastActionType: "FIRE",
      lastActionUid: ALICE_UID,
      lastTankX: 340,
      lastTankY: 495,
      lastFacingRight: true,
      lastAimAngle: 48,
      lastPower: 0.75,
      lastWeapon: "POOP",
      lastUsedItem: "SHIELD",
      updatedAt: t4,
    })
  );

  // 7. Finish match & verify terminal state lock
  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_1").update({
      status: "FINISHED",
      winnerUid: ALICE_UID,
      lastActionType: "FIRE",
      lastActionUid: ALICE_UID,
      updatedAt: t5,
    })
  );

  // Post-terminal mutation MUST fail
  await assertFails(
    bobDb.collection("match_rooms").doc("room_1").update({
      winnerUid: BOB_UID,
      status: "FINISHED",
      lastActionType: "FORFEIT",
      lastActionUid: BOB_UID,
      updatedAt: t5,
    })
  );
});

test("RoomInvite: host sends invite for existing room; recipient accepts; outsider denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = new Date(Date.now() - 8000);
  const updated = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("match_rooms").doc("room_inv").set(validRoomPayload(ALICE_UID, created))
  );

  // Cannot invite to non-existent room
  await assertFails(
    aliceDb.collection("room_invites").doc("inv_bad").set({
      roomId: "non_existent_room",
      roomCode: "K7M2P9",
      roomTitle: "초원 한판 붙자!",
      mapTheme: "GRASSLAND",
      fromUid: ALICE_UID,
      fromName: "블루캡틴",
      toUid: BOB_UID,
      toName: "레드불독",
      status: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid invite creation
  await assertSucceeds(
    aliceDb.collection("room_invites").doc("inv_1").set({
      roomId: "room_inv",
      roomCode: "K7M2P9",
      roomTitle: "초원 한판 붙자!",
      mapTheme: "GRASSLAND",
      fromUid: ALICE_UID,
      fromName: "블루캡틴",
      toUid: BOB_UID,
      toName: "레드불독",
      status: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Recipient lists own invites
  await assertSucceeds(
    bobDb.collection("room_invites").where("toUid", "==", BOB_UID).get()
  );

  // Outsider cannot read invite
  await assertFails(charlieDb.collection("room_invites").doc("inv_1").get());

  // Recipient accepts invite
  await assertSucceeds(
    bobDb.collection("room_invites").doc("inv_1").update({
      status: "ACCEPTED",
      updatedAt: updated,
    })
  );
});
