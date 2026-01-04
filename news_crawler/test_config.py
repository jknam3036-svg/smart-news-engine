#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
local.properties 설정 테스트 스크립트
실행: python test_config.py
"""
import os
import sys

def load_local_properties():
    """main.py와 동일한 로직"""
    props = {}
    try:
        paths = ["local.properties", "../local.properties", "../../local.properties"]
        for p in paths:
            if os.path.exists(p):
                print(f"[OK] 파일 발견: {os.path.abspath(p)}")
                with open(p, 'r', encoding='utf-8') as f:
                    for line_num, line in enumerate(f, 1):
                        if "=" in line and not line.strip().startswith("#"):
                            key, val = line.strip().split("=", 1)
                            props[key.strip()] = val.strip()
                            # 민감 정보 마스킹
                            if 'key' in key.lower() or 'Key' in key:
                                masked_val = val[:10] + "..." if len(val) > 10 else "***"
                                print(f"   Line {line_num}: {key.strip()} = {masked_val}")
                            else:
                                print(f"   Line {line_num}: {key.strip()} = {val.strip()}")
                break
        else:
            print("[ERROR] local.properties 파일을 찾을 수 없습니다.")
            print(f"   검색 경로: {[os.path.abspath(p) for p in paths]}")
            return None
    except Exception as e:
        print(f"[ERROR] 파일 읽기 오류: {e}")
        return None
    return props

def test_config():
    print("=" * 60)
    print("local.properties 설정 점검")
    print("=" * 60)
    
    # 1. 파일 읽기
    props = load_local_properties()
    if not props:
        print("\n[WARNING] 설정 파일이 없거나 읽을 수 없습니다.")
        return False
    
    print(f"\n[INFO] 총 {len(props)}개의 설정 항목 발견\n")
    
    # 2. Gemini API Key 확인
    print("-" * 60)
    print("[CHECK] Gemini API Key 확인")
    print("-" * 60)
    
    gemini_key = props.get('geminiKey') or props.get('GEMINI_API_KEY')
    
    if gemini_key:
        print(f"[OK] Gemini API Key 발견!")
        print(f"   키명: {'geminiKey' if 'geminiKey' in props else 'GEMINI_API_KEY'}")
        print(f"   길이: {len(gemini_key)} 문자")
        
        # 유효성 간단 체크
        if gemini_key.startswith('AIzaSy') and len(gemini_key) == 39:
            print(f"   형식: [OK] 올바른 Gemini API Key 형식")
        else:
            print(f"   형식: [WARNING] 비정상적인 형식 (AIzaSy로 시작하고 39자여야 함)")
    else:
        print("[ERROR] Gemini API Key를 찾을 수 없습니다!")
        print("   다음 중 하나를 추가하세요:")
        print("   - geminiKey=AIzaSy...")
        print("   - GEMINI_API_KEY=AIzaSy...")
        return False
    
    # 3. 환경 변수 확인
    print("\n" + "-" * 60)
    print("[CHECK] 환경 변수 확인 (우선순위 높음)")
    print("-" * 60)
    
    env_key = os.environ.get('GEMINI_API_KEY')
    if env_key:
        print(f"[OK] 환경 변수 GEMINI_API_KEY 설정됨 (이것이 우선 사용됨)")
        print(f"   길이: {len(env_key)} 문자")
    else:
        print(f"[INFO] 환경 변수 없음 (local.properties 사용)")
    
    print("\n" + "=" * 60)
    print("[SUCCESS] 설정 점검 완료!")
    print("=" * 60)
    return True

if __name__ == "__main__":
    success = test_config()
    sys.exit(0 if success else 1)
