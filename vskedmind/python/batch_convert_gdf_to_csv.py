import os
import mne
import pandas as pd
from pathlib import Path

# ========== 配置区域 ==========
# 输入目录：放 .gdf 文件的路径（默认是当前目录）
input_dir = '.'

# 是否跳过已存在的 CSV（True=跳过，False=覆盖）
skip_existing = True
# =================================

def convert_single_gdf(gdf_path):
    csv_path = str(gdf_path).replace('.gdf', '.csv')
    
    # 如果选择跳过已存在且文件存在，就直接跳过
    if skip_existing and os.path.exists(csv_path):
        print(f"跳过（已存在）: {gdf_path.name}")
        return True
    
    try:
        print(f"正在转换: {gdf_path.name}")
        raw = mne.io.read_raw_gdf(str(gdf_path), preload=True, verbose=False)
        df = raw.to_data_frame(time_format='ms')
        df.to_csv(csv_path, index=False, encoding='utf-8')
        print(f"  完成: {csv_path}")
        return True
    except Exception as e:
        print(f"  失败: {gdf_path.name} - {e}")
        return False

def main():
    # 扫描当前目录下所有的 .gdf 文件
    gdf_files = sorted(Path(input_dir).glob('*.gdf'))
    
    if not gdf_files:
        print("当前目录下没有找到 .gdf 文件！")
        return
    
    print(f"找到 {len(gdf_files)} 个 GDF 文件，开始批量转换...\n")
    
    success_count = 0
    fail_count = 0
    
    for gdf_file in gdf_files:
        if convert_single_gdf(gdf_file):
            success_count += 1
        else:
            fail_count += 1
    
    print(f"\n转换完成！")
    print(f"成功: {success_count}")
    print(f"失败: {fail_count}")

if __name__ == '__main__':
    main()
