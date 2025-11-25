import { setGlobalOptions } from "firebase-functions/v2";
import { initializeApp } from "firebase-admin/app";

setGlobalOptions({ maxInstances: 3 });

initializeApp();
