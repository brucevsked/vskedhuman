import mne
import pandas as pd
import numpy as np

# ========== 只转换一个文件调试 ==========
gdf_file = 'A01T.gdf'
csv_file = 'A01T_debug.csv'
# =================================

print("=" * 60)
print("步骤1：读取 GDF")
print("=" * 60)

raw = mne.io.read_raw_gdf(gdf_file, preload=True, verbose=False)

print("步骤2：从注释中提取事件")
events, event_dict = mne.events_from_annotations(raw, verbose=False)

print(f"事件字典: {event_dict}")

# 步骤2.5：创建反向映射（MNE码 → 原始事件码）
# event_dict 的 key 是字符串，value 是整数
# 比如: '769': 7 表示 MNE 码 7 对应原始事件码 769

reverse_mapping = {}
for orig_code_str, mapped_code in event_dict.items():
    orig_code = int(orig_code_str)
    reverse_mapping[mapped_code] = orig_code

print(f"反向映射: {reverse_mapping}")

print("\n步骤3：导出到 CSV（含真实事件码）")

df = raw.to_data_frame(time_format='ms')

# 创建事件列（初始全为0）
event_series = pd.Series(0, index=df.index, name='stimulus')

# 填入事件（使用反向映射还原真实事件码）
for event in events:
    sample_idx, _, mapped_code = event
    if sample_idx < len(df):
        # 还原成真实的事件码（769, 770, 771, 772 等）
        real_code = reverse_mapping.get(mapped_code, 0)
        event_series.iloc[sample_idx] = real_code

# 拼接
df_with_events = pd.concat([df, event_series], axis=1)

# 保存
df_with_events.to_csv(csv_file, index=False, encoding='utf-8')
print(f"✅ 已保存到: {csv_file}")

# 统计事件
print("\n步骤4：验证事件分布")
event_rows = df_with_events[df_with_events['stimulus'] != 0]
print(f"共有 {len(event_rows)} 行包含事件标记")

# 统计各类事件的数量
event_counts = event_rows['stimulus'].value_counts().sort_index()
print("\n事件统计：")
for code, count in event_counts.items():
    label_map = {769: '左手', 770: '右手', 771: '脚', 772: '舌', 768: '试验开始'}
    label = label_map.get(int(code), f'未知({code})')
    print(f"  {code} ({label}): {count} 个")

# 打印几个样本事件位置
print("\n前10个事件行：")
print(event_rows.iloc[:10, -5:])
