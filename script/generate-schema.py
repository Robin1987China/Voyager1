#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Voyager1 数据库建表脚本生成器

读取 sql-view/ 下的 table.*.csv + alter.*.csv + index.*.csv，
按版本顺序合并变更（ADD/DROP/ALTER/DROP-TABLE），
生成一份「干净合并版」的 H2 建表 SQL（已去除历史废弃字段与废弃表）。

用法：
    python3 script/generate-schema.py [输出文件路径]
默认输出：docs/schema-h2.sql
"""
import csv
import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SQL_VIEW = os.path.join(ROOT, 'modules/server/src/main/resources/sql-view')
OUTPUT = os.path.join(ROOT, 'docs/schema-h2.sql')


def s(v):
    return (v or '').strip()


def read_csv(path):
    with open(path, encoding='utf-8') as f:
        return list(csv.DictReader(f))


def ver_key(path):
    m = re.search(r'v(\d+)\.(\d+)(?:\.(\d+))?', path)
    return tuple(int(x) for x in m.groups() if x is not None)


TYPE_MAP = {
    'LONG': 'BIGINT',
    'STRING': 'VARCHAR',
    'TEXT': 'CLOB',
    'INTEGER': 'INTEGER',
    'TINYINT': 'TINYINT',
    'FLOAT': 'REAL',
    'DOUBLE': 'DOUBLE',
}


def col_sql(field):
    name = s(field.get('name'))
    typ = s(field.get('type')).upper()
    sql_type = TYPE_MAP.get(typ, typ)
    if sql_type == 'VARCHAR':
        sql_type = 'VARCHAR({})'.format(s(field.get('len')) or 255)
    parts = ['`{}`'.format(name), sql_type]
    if s(field.get('notNull')).lower() == 'true':
        parts.append('not null')
    if s(field.get('defaultValue')):
        parts.append("default '{}'".format(s(field.get('defaultValue'))))
    if s(field.get('comment')):
        parts.append("comment '{}'".format(s(field.get('comment'))))
    return ' '.join(parts)


def build():
    # 1. 累积 table CSV
    tables = {}
    table_files = (
        sorted(glob.glob(os.path.join(SQL_VIEW, 'table.all.v*.csv')), key=ver_key)
        + sorted(glob.glob(os.path.join(SQL_VIEW, 'table.h2.v*.csv')), key=ver_key)
    )
    for tf in sorted(table_files, key=ver_key):
        for row in read_csv(tf):
            tn = s(row.get('tableName'))
            if not tn:
                continue
            if tn not in tables:
                tables[tn] = {'comment': s(row.get('tableComment')), 'fields': {}}
            fn = s(row.get('name'))
            if fn:
                tables[tn]['fields'][fn] = row

    # 2. 应用 alter 变更
    for af in sorted(glob.glob(os.path.join(SQL_VIEW, 'alter.all.v*.csv')), key=ver_key):
        for row in read_csv(af):
            at = s(row.get('alterType')).upper()
            tn = s(row.get('tableName'))
            fn = s(row.get('name'))
            if at == 'ADD' and tn in tables:
                tables[tn]['fields'][fn] = row
            elif at == 'DROP' and tn in tables:
                tables[tn]['fields'].pop(fn, None)
            elif at == 'DROP-TABLE':
                tables.pop(tn, None)
            elif at == 'ALTER' and tn in tables and fn in tables[tn]['fields']:
                old = tables[tn]['fields'][fn]
                if s(row.get('type')):
                    old['type'] = s(row['type'])
                if s(row.get('len')):
                    old['len'] = s(row['len'])
                if s(row.get('notNull')):
                    old['notNull'] = s(row['notNull'])
                if s(row.get('comment')):
                    old['comment'] = s(row['comment'])

    # 3. 生成建表 SQL
    lines = [
        '-- Voyager1 数据库建表脚本（H2，干净合并版）',
        '-- 由 sql-view/table.*.csv + alter.*.csv 合并生成，已去除历史废弃字段/表',
        '',
    ]
    for tn in sorted(tables):
        t = tables[tn]
        fields = list(t['fields'].values())
        pk = [s(f.get('name')) for f in fields if s(f.get('primaryKey')).lower() == 'true']
        lines.append('CREATE TABLE IF NOT EXISTS PUBLIC.{}'.format(tn))
        lines.append('(')
        for f in fields:
            lines.append('    {},'.format(col_sql(f)))
        lines.append('    CONSTRAINT {}_PK PRIMARY KEY ({})'.format(tn, ', '.join(pk) if pk else 'id'))
        lines.append(');')
        lines.append("COMMENT ON TABLE {} is '{}';".format(tn, t['comment'] or tn))
        lines.append('')

    # 4. 索引
    idx_lines = []
    for row in read_csv(os.path.join(SQL_VIEW, 'index.all.v1.0.csv')):
        it = s(row.get('indexType'))
        tn = s(row.get('tableName'))
        name = s(row.get('name'))
        fields = [x.strip() for x in s(row.get('field')).split('+') if x.strip()]
        if it == 'ADD-UNIQUE':
            idx_lines.append('CREATE UNIQUE INDEX IF NOT EXISTS {} ON PUBLIC.{} ({});'.format(name, tn, ', '.join(fields)))
        elif it == 'ADD':
            idx_lines.append('CREATE INDEX IF NOT EXISTS {} ON PUBLIC.{} ({});'.format(name, tn, ', '.join(fields)))

    out = '\n'.join(lines) + '\n'.join(idx_lines) + '\n'
    return out, len(tables), len(idx_lines)


if __name__ == '__main__':
    target = sys.argv[1] if len(sys.argv) > 1 else OUTPUT
    sql, table_count, idx_count = build()
    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(target, 'w', encoding='utf-8') as f:
        f.write(sql)
    print('已生成 {}：{} 张表，{} 个索引，{} 字节'.format(target, table_count, idx_count, len(sql)))
