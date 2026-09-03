#!/usr/bin/env python3
"""由 sql-view CSV 生成 Flyway V1__init.sql（H2 兼容 DDL）。

用法: python3 script/gen-flyway-init.py
输出: modules/server/src/main/resources/db/migration/V1__init.sql
"""
import csv
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_ALL = os.path.join(ROOT, "modules/server/src/main/resources/sql-view/table.all.v1.0.csv")
CSV_H2 = os.path.join(ROOT, "modules/server/src/main/resources/sql-view/table.h2.v1.0.csv")
OUT = os.path.join(ROOT, "modules/server/src/main/resources/db/migration/V1__init.sql")


def sql_type(t, ln):
    t = t.strip().lower()
    if t == "string":
        return f"VARCHAR({ln})" if ln else "VARCHAR(255)"
    if t == "long":
        return "BIGINT"
    if t == "integer":
        return "INTEGER"
    if t == "tinyint":
        return "TINYINT"
    if t == "text":
        return "TEXT"
    if t == "double":
        return "DOUBLE"
    if t == "float":
        return "FLOAT"
    return "VARCHAR(255)"


def read_tables(csv_path):
    tables = {}
    with open(csv_path, encoding="utf-8") as f:
        r = csv.reader(f)
        next(r, None)  # header
        for row in r:
            if len(row) < 7:
                continue
            tname, col, typ, ln = row[0], row[1], row[2], row[3]
            default = row[4] if len(row) > 4 else ""
            notnull = row[5].strip().lower() == "true" if len(row) > 5 else False
            pk = row[6].strip().lower() == "true" if len(row) > 6 else False
            tables.setdefault(tname, []).append((col, sql_type(typ, ln), default, notnull, pk))
    return tables


def main():
    tables = {}
    for p in (CSV_ALL, CSV_H2):
        for k, v in read_tables(p).items():
            tables.setdefault(k, []).extend(v)

    lines = [
        "-- Voyager1 初始 schema（由 sql-view CSV 生成，Flyway V1 基线）",
        "-- H2 MODE=MYSQL; 标识符不加引号（与旧建表行为一致，H2 存储为大写）",
        "",
    ]
    for tname, cols in tables.items():
        lines.append(f"CREATE TABLE IF NOT EXISTS {tname} (")
        col_lines = []
        for col, typ, default, notnull, pk in cols:
            s = f"  {col} {typ}"
            if default not in ("", None):
                s += f" DEFAULT {default}"
            if notnull:
                s += " NOT NULL"
            col_lines.append(s)
        pk_cols = [c for c in cols if c[4]]
        if pk_cols:
            col_lines.append("  PRIMARY KEY (" + ", ".join(c[0] for c in pk_cols) + ")")
        lines.append(",\n".join(col_lines))
        lines.append(");")
        lines.append("")

    with open(OUT, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"generated {OUT}: {len(tables)} tables")


if __name__ == "__main__":
    main()
