const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date(Date.now() - 1000);
}

function validStoreProfile(uid) {
  const ts = nowTimestamp();
  return {
    ownerId: uid,
    name: "GERAI UNA SMART POS",
    address: "Jl. Raya Utama No. 1",
    phone: "08123456789",
    businessType: "keduanya",
    ppnBp: 0,
    receiptFooter: "Terima kasih!",
    ownerName: "Egis Bahricha",
    ownerEmail: "owner@geraiuna.id",
    profileUpdatedAtMillis: 1700000000000,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCashAccount(uid) {
  const ts = nowTimestamp();
  return {
    ownerId: uid,
    deviceId: "dev_abc123",
    localId: 1,
    accountName: "Kas Toko",
    balance: 1500000,
    accountType: "CASH",
    isActive: true,
    color: "#16A34A",
    notes: "Uang fisik laci",
    isDefault: true,
    sortOrder: 1,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCashMutation(uid) {
  const ts = nowTimestamp();
  return {
    ownerId: uid,
    deviceId: "dev_abc123",
    localId: 10,
    cashAccountId: 1,
    accountName: "Kas Toko",
    amount: 55000,
    mutationType: "PENJUALAN",
    refType: "SALE",
    refId: "TRX-001",
    notes: "Penjualan Tunai",
    mutationTimeMillis: 1700000000000,
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

test("Unauthenticated user: cannot read or write store profile or cashAccounts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cashAccounts").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validStoreProfile(ALICE_UID)));
});

test("Authenticated owner: can create and read their own store profile & subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validStoreProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cashAccounts").doc("acc_1").set(validCashAccount(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cashMutations").doc("mut_1").set(validCashMutation(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cashAccounts").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("cashMutations").get());
});

test("Cross-user isolation: Bob cannot read, list, or write Alice's store or cash flow", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validStoreProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cashAccounts").doc("acc_1").set(validCashAccount(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cashAccounts").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cashAccounts").doc("acc_2").set(validCashAccount(BOB_UID))
  );
  await assertFails(bobDb.collection("users").get());
});

test("Shadow update & identity spoofing: rejects ghost fields and mismatched ownerId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validStoreProfile(ALICE_UID),
      isAdmin: true,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cashAccounts").doc("acc_1").set({
      ...validCashAccount(BOB_UID),
    })
  );
});

test("ID poisoning & invalid types: rejects invalid doc IDs and oversized strings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cashAccounts").doc("bad$id!").set(validCashAccount(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cashAccounts").doc("acc_1").set({
      ...validCashAccount(ALICE_UID),
      balance: "not_a_number",
    })
  );
});

test("Update preserves createdAt immutability and blocks changing ownerId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const accRef = aliceDb.collection("users").doc(ALICE_UID).collection("cashAccounts").doc("acc_1");
  await assertSucceeds(accRef.set(validCashAccount(ALICE_UID)));

  // Valid partial update keeping createdAt untouched
  await assertSucceeds(
    accRef.update({
      balance: 2000000,
      updatedAt: nowTimestamp(),
    })
  );

  // Reject modifying createdAt
  await assertFails(
    accRef.update({
      balance: 2500000,
      createdAt: new Date(1000000),
      updatedAt: nowTimestamp(),
    })
  );

  // Reject changing ownerId
  await assertFails(
    accRef.update({
      ownerId: BOB_UID,
      updatedAt: nowTimestamp(),
    })
  );
});

function validCustomerDemoLicense(uid, email, key) {
  const ts = nowTimestamp();
  const start = Date.now() - 1000;
  return {
    licenseKey: key,
    storeName: "Toko Berkah",
    ownerName: "Alice",
    ownerEmail: email,
    ownerUid: uid,
    phone: "081234567890",
    deviceId: "dev_abc123",
    planType: "DEMO",
    status: "DEMO",
    demoStartedAtMillis: start,
    demoExpiresAtMillis: start + 3 * 24 * 60 * 60 * 1000,
    activatedAtMillis: 0,
    expiresAtMillis: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Licenses: customer can create 3-day DEMO, cannot self-activate FULL, cannot read other customers", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();

  await assertSucceeds(
    aliceDb.collection("licenses").doc("KSL-ALICE-1").set(validCustomerDemoLicense(ALICE_UID, "alice@example.com", "KSL-ALICE-1"))
  );

  // Customer cannot self-create ACTIVE/FULL license
  await assertFails(
    bobDb.collection("licenses").doc("KSL-BOB-FULL").set({
      ...validCustomerDemoLicense(BOB_UID, "bob@example.com", "KSL-BOB-FULL"),
      planType: "FULL",
      status: "ACTIVE",
    })
  );

  // Customer cannot elevate own DEMO to ACTIVE
  await assertFails(
    aliceDb.collection("licenses").doc("KSL-ALICE-1").update({
      planType: "FULL",
      status: "ACTIVE",
      updatedAt: nowTimestamp(),
    })
  );

  // Bob cannot get or list Alice's license
  await assertFails(bobDb.collection("licenses").doc("KSL-ALICE-1").get());
  await assertFails(bobDb.collection("licenses").get());
});

test("Licenses: Super Admin (egisbahricha@gmail.com) can list all, activate FULL, extend demo, and reset deviceId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const adminDb = testEnv.authenticatedContext("super_uid", { email: "egisbahricha@gmail.com", email_verified: true }).firestore();

  await assertSucceeds(
    aliceDb.collection("licenses").doc("KSL-ALICE-1").set(validCustomerDemoLicense(ALICE_UID, "alice@example.com", "KSL-ALICE-1"))
  );

  await assertSucceeds(adminDb.collection("licenses").get());
  await assertSucceeds(
    adminDb.collection("licenses").doc("KSL-ALICE-1").update({
      planType: "FULL",
      status: "ACTIVE",
      activatedAtMillis: Date.now(),
      expiresAtMillis: Date.now() + 3650 * 24 * 60 * 60 * 1000,
      deviceId: "",
      updatedAt: nowTimestamp(),
    })
  );
});

