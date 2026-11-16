const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_1";
const BOB_UID = "bob_owner_2";

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

test("Unauthenticated caller cannot read or write user profile or subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("orders").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("inventory").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("staff").get()
  );
});

test("Owner can create and update RestaurantProfile, while cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      restaurantName: "Saffron Haveli",
      language: "en",
      gstPercent: 5,
      dailyTarget: 25000,
      autoKotEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ restaurantName: "Hacked" })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .update({ restaurantName: "Saffron", isAdmin: true })
  );
});

test("MenuItem: owner create/list succeeds, invalid price or cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const itemRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("menu_items")
    .doc("dish_1");

  await assertSucceeds(
    itemRef.set({
      name: "Paneer Tikka",
      category: "Starters",
      price: 320,
      costPrice: 120,
      veg: true,
      available: true,
      prepMinutes: 15,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("menu_items")
      .get()
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("menu_items")
      .get()
  );

  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("menu_items")
      .doc("dish_bad")
      .set({
        name: "Negative Dish",
        category: "Starters",
        price: -10,
        veg: true,
        available: true,
        createdAt: now,
        updatedAt: now,
      })
  );
});

test("OrderRecord: lifecycle updates work while COMPLETED locks financial edits except print flags", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const orderRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("orders")
    .doc("ord_1");

  await assertSucceeds(
    orderRef.set({
      orderNumber: "ORD-101",
      channel: "ZOMATO",
      tableOrRef: "ZOM-881",
      itemsSummary: "2x Butter Naan, 1x Dal Makhani",
      itemsCount: 3,
      subtotal: 450,
      gstEnabled: true,
      gstPercent: 5,
      gstAmount: 22.5,
      totalAmount: 472.5,
      tipAmount: 30,
      paymentMethod: "ONLINE_PREPAID",
      status: "RUNNING",
      kotPrinted: true,
      billPrinted: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Transition RUNNING -> COMPLETED succeeds
  await assertSucceeds(
    orderRef.update({
      status: "COMPLETED",
      updatedAt: now,
    })
  );

  // Printing bill on COMPLETED succeeds
  await assertSucceeds(
    orderRef.update({
      billPrinted: true,
      updatedAt: now,
    })
  );

  // Tampering totalAmount after COMPLETED is blocked
  await assertFails(
    orderRef.update({
      totalAmount: 1,
      updatedAt: now,
    })
  );
});

test("Staff, Attendance, Inventory, Expenses, PartyBookings enforce owner isolation and schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("staff").doc("st_1").set({
      name: "Gurpreet Singh",
      role: "Tandoor Master",
      monthlySalary: 24000,
      dailyFoodAllowance: 150,
      shiftHours: 10,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("attendance")
      .doc("att_1")
      .set({
        staffId: "st_1",
        staffName: "Gurpreet Singh",
        role: "Tandoor Master",
        dateStr: "2026-11-16",
        checkInTimeStr: "09:15 AM",
        status: "PRESENT",
        tipEarned: 250,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("inventory")
      .doc("inv_1")
      .set({
        name: "Tandoor Coal",
        category: "TANDOOR_COAL",
        quantity: 45,
        unit: "kg",
        minThreshold: 20,
        costPerUnit: 40,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("expenses")
      .doc("exp_1")
      .set({
        title: "Commercial Gas Cylinder",
        category: "MATERIAL_GAS",
        amount: 1950,
        dateStr: "2026-11-16",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("party_bookings")
      .doc("pty_1")
      .set({
        partyName: "Sharma Anniversary",
        partyPhone: "9876543210",
        eventDate: "2026-11-20",
        guestCount: 60,
        totalBudget: 35000,
        advancePaid: 10000,
        inventoryUsedSummary: "Paneer 10kg, Cola 2 crates",
        chefRequirements: "15kg Basmati, 5kg Butter, 30kg Coal",
        status: "CONFIRMED",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Cross-user access denied on all subcollections
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("staff").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("attendance").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("inventory").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("expenses").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("party_bookings").get()
  );
});
