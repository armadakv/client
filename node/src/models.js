export class KeyValue {
  constructor(key, value) {
    this.key = key;
    this.value = value;
  }
}

export class GetResult {
  constructor({ kvs = [], count = 0, revision = 0 } = {}) {
    this.kvs = kvs;
    this.count = count;
    this.revision = revision;
  }
}

export class PutResult {
  constructor({ revision = 0, prevKv = null } = {}) {
    this.revision = revision;
    this.prevKv = prevKv;
  }
}

export class DeleteResult {
  constructor({ revision = 0, deleted = 0, prevKvs = [] } = {}) {
    this.revision = revision;
    this.deleted = deleted;
    this.prevKvs = prevKvs;
  }
}

export class TxnResult {
  constructor({ revision = 0, succeeded = false, responses = [] } = {}) {
    this.revision = revision;
    this.succeeded = succeeded;
    this.responses = responses;
  }
}

export class TableInfo {
  constructor(name) {
    this.name = name;
  }
}

export class ClusterMember {
  constructor({ id, name, peerUrls = [], clientUrls = [] }) {
    this.id = id;
    this.name = name;
    this.peerUrls = peerUrls;
    this.clientUrls = clientUrls;
  }
}
