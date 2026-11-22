const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const ADMIN_UID = "admin_uid_003";
const ADMIN_EMAIL = "tagreesmeralda@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("incidents").get());
  await assertFails(unauthDb.collection("paramedic_cards").doc("VQR_ALICE_1").get());
  await assertFails(
    unauthDb.collection("incidents").doc("inc_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      bloodType: "O+",
      vehicleType: "MOTO",
      department: "Guatemala",
      latitude: 14.6349,
      longitude: -90.5069,
      status: "ACTIVA",
      severity: "ALTA",
      minimumFeeGtq: 150.0,
      notifiedContactsCount: 1,
      offlineQueued: false,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Owner can create and update UserProfile, Contacts, Vehicles, and Payments; cross-user access denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Create UserProfile (mirrors Kotlin payload without @DocumentId id)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "María Pérez",
      email: "maria@example.com",
      phone: "+50255551234",
      dpi: "2345678900101",
      bloodType: "O+",
      allergies: "Penicilina",
      conditions: "Ninguna",
      medications: "Ninguno",
      insuranceInfo: "IGSS",
      organDonor: true,
      department: "Guatemala",
      minimumFeeGtq: 150.0,
      shareMedicalInQr: true,
      privacyAccepted: true,
      qrToken: "VQR-ALICE-001",
      totpSecret: "JBSWY3DPEHPK3PXP",
      totpEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update on UserProfile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      minimumFeeGtq: 200.0,
      allergies: "Penicilina, Sulfas",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp must fail
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob",
      bloodType: "A+",
      department: "Sacatepéquez",
      minimumFeeGtq: 100.0,
      shareMedicalInQr: true,
      privacyAccepted: true,
      qrToken: "VQR-BOB-001",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Emergency Contact subcollection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").doc("c_1").set({
      userId: ALICE_UID,
      name: "Carlos Pérez",
      phone: "+50244449999",
      relationship: "Hermano",
      notifyAuto: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("contacts").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("contacts").get()
  );

  // Vehicle Record subcollection
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("v_1").set({
      userId: ALICE_UID,
      vehicleType: "MOTO",
      plate: "M-458XYZ",
      brandModel: "Honda XR150",
      color: "Rojo",
      aiVerified: true,
      aiConfidence: 95,
      aiDetectedType: "MOTO",
      aiSummary: "Motocicleta real verificada",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").get()
  );

  // Encrypted tokenized payment record (immutable after create)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("payments").doc("pay_1").set({
      userId: ALICE_UID,
      amountGtq: 200.0,
      gatewayName: "Visanet GT 3DS",
      maskedLast4: "4242",
      tokenReference: "tok_tls_991188",
      encryptionProtocol: "TLS 1.3 + AES-256-GCM",
      status: "APROBADO",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("payments").doc("pay_1").update({
      amountGtq: 1.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Incidents enforce ownership, admin privileges, verified email check, and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("incidents").doc("inc_alice_1").set({
      userId: ALICE_UID,
      userName: "María Pérez",
      bloodType: "O+",
      allergies: "Penicilina",
      vehicleType: "CARRO",
      department: "Guatemala",
      latitude: 14.6349,
      longitude: -90.5069,
      accuracyMeters: 8.5,
      status: "ACTIVA",
      severity: "CRITICA",
      minimumFeeGtq: 150.0,
      notifiedContactsCount: 2,
      offlineQueued: false,
      notes: "Colisión leve en Calzada Roosevelt",
      assignedUnit: "Pendiente",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered user query succeeds; unfiltered query fails for non-admin
  await assertSucceeds(
    aliceDb.collection("incidents").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("incidents").get());
  await assertFails(bobDb.collection("incidents").doc("inc_alice_1").get());

  // Spoofed unverified admin email MUST fail
  await assertFails(spoofAdminDb.collection("incidents").get());
  await assertFails(spoofAdminDb.collection("incidents").doc("inc_alice_1").get());

  // Verified admin can list all incidents and dispatch unit
  await assertSucceeds(adminDb.collection("incidents").get());
  await assertSucceeds(
    adminDb.collection("incidents").doc("inc_alice_1").update({
      status: "EN_RUTA",
      assignedUnit: "Bomberos Voluntarios Cia. 10",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Paramedic cards allow exact token get when signed in but forbid collection list scraping", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("paramedic_cards").doc("VQR_ALICE_001").set({
      userId: ALICE_UID,
      cardToken: "VQR_ALICE_001",
      fullName: "María Pérez",
      bloodType: "O+",
      allergies: "Penicilina",
      conditions: "Ninguna",
      medications: "Ninguno",
      primaryContactName: "Carlos Pérez",
      primaryContactPhone: "+50244449999",
      vehicleSummary: "MOTO · M-458XYZ",
      department: "Guatemala",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Signed-in paramedic can read specific QR card by exact token
  await assertSucceeds(bobDb.collection("paramedic_cards").doc("VQR_ALICE_001").get());
  // Collection listing / scraping is strictly forbidden
  await assertFails(bobDb.collection("paramedic_cards").get());
  // Bob cannot overwrite Alice's card
  await assertFails(
    bobDb.collection("paramedic_cards").doc("VQR_ALICE_001").update({
      bloodType: "AB-",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
