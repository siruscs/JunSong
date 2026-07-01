#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RapidOCR 票据识别脚本（基于 PaddleOCR ONNX 模型）
用法: python3 paddle_ocr.py <图片路径>
输出: JSON 格式 {"text": "识别的文本内容"}
"""

import sys
import json
import os

def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "缺少图片路径参数"}))
        sys.exit(1)

    image_path = sys.argv[1]
    if not os.path.exists(image_path):
        print(json.dumps({"error": "图片文件不存在: " + image_path}))
        sys.exit(1)

    try:
        from rapidocr_onnxruntime import RapidOCR

        # 初始化OCR引擎（自动下载默认中英文模型，CPU模式）
        ocr = RapidOCR()

        result, _ = ocr(image_path)

        if not result:
            print(json.dumps({"text": ""}))
            return

        # RapidOCR 返回 [[box, text, confidence], ...]
        # 按行拼接文本
        lines = []
        for item in result:
            text = item[1] if len(item) > 1 else ""
            if text:
                lines.append(text)

        full_text = "\n".join(lines)
        print(json.dumps({"text": full_text}, ensure_ascii=False))

    except ImportError:
        print(json.dumps({"error": "rapidocr_onnxruntime 模块未安装"}))
        sys.exit(2)
    except Exception as e:
        print(json.dumps({"error": str(e)}))
        sys.exit(3)

if __name__ == "__main__":
    main()
