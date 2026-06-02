#!/usr/bin/python3
import os
import sys
import subprocess
from PIL import Image

# --- 配置區 ---
TARGET_WIDTH = 640
REQUIRED_LIBS = ["Pillow"]
VENV_DIR = ".venv"
# 擴展支援的副檔名
VALID_EXTENSIONS = ('.jpg', '.jpeg', '.JPG', '.JPEG', '.png', '.PNG')

def setup_venv():
    """檢查並設定虛擬環境與所需套件。若不在虛擬環境內，則自動安裝並重新導向執行。"""
    # 判斷目前是否在虛擬環境內 (檢查 sys.prefix 或 VIRTUAL_ENV 變數)
    is_in_venv = sys.prefix != sys.base_prefix or 'VIRTUAL_ENV' in os.environ
    
    # 決定虛擬環境的 Python 路徑
    if os.name == 'nt':  # Windows
        venv_python = os.path.join(VENV_DIR, "Scripts", "python.exe")
    else:  # Linux / macOS
        venv_python = os.path.join(VENV_DIR, "bin", "python")

    if not is_in_venv:
        # 1. 檢查虛擬環境是否存在，不存在就建立
        if not os.path.exists(VENV_DIR):
            print(f"[*] 建立虛擬環境 {VENV_DIR}...")
            subprocess.check_call([sys.executable, "-m", "venv", VENV_DIR])
            
            print("[*] 升級虛擬環境的 pip 並安裝必要套件...")
            subprocess.check_call([venv_python, "-m", "pip", "install", "--upgrade", "pip"])
            subprocess.check_call([venv_python, "-m", "pip", "install"] + REQUIRED_LIBS)
            print("-" * 40)
        
        # 2. 關鍵改動：直接用虛擬環境的 Python 重新執行本腳本，傳遞原本的所有參數 (sys.argv)
        # os.execv 會直接取代當前的行程 (Process)，不會殘留原本的非虛擬環境進程
        os.execv(venv_python, [venv_python] + sys.argv)

def resize_images(directory):
    """核心轉換邏輯"""
    count = 0
    for filename in os.listdir(directory):
        if filename.endswith(VALID_EXTENSIONS):
            file_path = os.path.join(directory, filename)
            try:
                with Image.open(file_path) as img:
                    orig_width, orig_height = img.size
                    
                    # 計算比例
                    ratio = TARGET_WIDTH / float(orig_width)
                    target_height = int(float(orig_height) * float(ratio))
                    
                    # 執行縮放
                    resized_img = img.resize((TARGET_WIDTH, target_height), Image.Resampling.LANCZOS)
                    
                    # 處理 PNG 的透明度問題 (若要覆蓋原檔且保持 PNG 格式)
                    # 如果原圖是 RGBA，我們保留它；如果要轉 JPG 才需要轉 RGB
                    # 這裡採直接覆蓋，所以維持原模式
                    resized_img.save(file_path, quality=95)
                    
                count += 1
                print(f"[OK] {filename} -> {TARGET_WIDTH}x{target_height}")
            except Exception as e:
                print(f"[ERROR] 處理 {filename} 失敗: {e}")
    return count

if __name__ == "__main__":
    # 自動處理虛擬環境與套件
    setup_venv()

    # 只有當成功切換至虛擬環境後，才會執行到這裡
    target_dir = os.getcwd()
    print(f"[*] 工作目錄: {target_dir}")
    print(f"[*] 支援格式: {', '.join(VALID_EXTENSIONS)}")
    
    confirm = input(f"[危險] 將覆蓋所有 JPG/PNG (寬度縮至 {TARGET_WIDTH})，確定嗎？ (y/n): ")
    if confirm.lower() == 'y':
        total = resize_images(target_dir)
        print(f"\n[完成] 處理了 {total} 張圖片。")
    else:
        print("[取消] 操作終止。")
