#!/usr/bin/env python3
"""把生成的 200 条新闻数据追加到 init.sql 中第 8 条 INSERT 之后。"""
import re

sql_path = 'e:/occupation/occupation-common/src/main/resources/sql/init.sql'
data_path = 'e:/occupation/occupation-common/src/main/resources/sql/upgrade-2026-07-13-news-bulk.sql'

with open(sql_path, 'r', encoding='utf-8') as f:
    sql = f.read()

with open(data_path, 'r', encoding='utf-8') as f:
    data = f.read()

# 提取 data 的 VALUES 块
m = re.search(r'\(9, 1,', data)
if not m:
    raise SystemExit("can't find data start")
start = m.start()
end = data.rfind(';', start)
data_block = data[start:end].strip()

# 在 init.sql 中找到 marker
marker = "-- ---------- 资讯扩充：200 条（5 分类 × 3 类型，全部附带 picsum.photos 真实封面图） ----------"
idx = sql.find(marker)
if idx < 0:
    raise SystemExit("can't find marker in init.sql")

header_end_marker = "INSERT INTO news (id, tenant_id, category, type, title, summary, content, cover_style, cover_image, source, source_url, link_target, view_count, featured, status, publish_time) VALUES\n"
hdr_idx = sql.find(header_end_marker, idx)
if hdr_idx < 0:
    raise SystemExit("can't find header end")

insert_pos = hdr_idx + len(header_end_marker)
new_sql = sql[:insert_pos] + data_block + ";\n\n" + sql[insert_pos:]

with open(sql_path, 'w', encoding='utf-8') as f:
    f.write(new_sql)

print(f"Appended {data_block.count('(')} rows into init.sql")
