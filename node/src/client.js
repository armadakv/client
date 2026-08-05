import { readFileSync } from "node:fs";
import { resolve } from "node:path";

import { ClusterMember, DeleteResult, GetResult, PutResult, TableInfo, TxnResult } from "./models.js";

export class ArmadaClient {
  constructor({ endpoint = null } = {}) {
    this.endpoint = endpoint;
  }

  tables() {
    return [];
  }

  get() {
    throw new Error("Node SDK scaffold does not talk to Armada yet");
  }

  put() {
    throw new Error("Node SDK scaffold does not talk to Armada yet");
  }

  delete() {
    throw new Error("Node SDK scaffold does not talk to Armada yet");
  }

  txn() {
    throw new Error("Node SDK scaffold does not talk to Armada yet");
  }

  memberList() {
    return [];
  }
}

export function loadCoreFixture(path) {
  const file = resolve(path);
  return JSON.parse(readFileSync(file, "utf8"));
}

export {
  ClusterMember,
  DeleteResult,
  GetResult,
  PutResult,
  TableInfo,
  TxnResult,
};
