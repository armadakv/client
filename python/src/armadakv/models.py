from dataclasses import dataclass, field
from typing import Any


@dataclass(frozen=True)
class KeyValue:
    key: str
    value: str


@dataclass(frozen=True)
class GetResult:
    kvs: list[KeyValue] = field(default_factory=list)
    count: int = 0
    revision: int = 0


@dataclass(frozen=True)
class PutResult:
    revision: int = 0
    prev_kv: KeyValue | None = None


@dataclass(frozen=True)
class DeleteResult:
    revision: int = 0
    deleted: int = 0
    prev_kvs: list[KeyValue] = field(default_factory=list)


@dataclass(frozen=True)
class TxnResult:
    revision: int = 0
    succeeded: bool = False
    responses: list[Any] = field(default_factory=list)


@dataclass(frozen=True)
class TableInfo:
    name: str


@dataclass(frozen=True)
class ClusterMember:
    id: str
    name: str
    peer_urls: list[str] = field(default_factory=list)
    client_urls: list[str] = field(default_factory=list)
