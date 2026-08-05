from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import json

from .models import ClusterMember, DeleteResult, GetResult, KeyValue, PutResult, TableInfo, TxnResult


@dataclass
class ArmadaClient:
    """Minimal Python client scaffold for the Armada core surface."""

    endpoint: str | None = None

    def tables(self) -> list[TableInfo]:
        return []

    def get(self, table: str, key: str) -> GetResult:
        raise NotImplementedError("Python SDK scaffold does not talk to Armada yet")

    def put(self, table: str, key: str, value: str) -> PutResult:
        raise NotImplementedError("Python SDK scaffold does not talk to Armada yet")

    def delete(self, table: str, key: str) -> DeleteResult:
        raise NotImplementedError("Python SDK scaffold does not talk to Armada yet")

    def txn(self, table: str) -> TxnResult:
        raise NotImplementedError("Python SDK scaffold does not talk to Armada yet")

    def member_list(self) -> list[ClusterMember]:
        return []


def load_core_fixture(path: str | Path) -> dict:
    with Path(path).open("r", encoding="utf-8") as f:
        return json.load(f)


__all__ = ["ArmadaClient", "load_core_fixture"]
