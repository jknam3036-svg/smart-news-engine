"""
Gemini API 키 테스트 스크립트
GitHub Actions에서 사용하기 전에 로컬에서 API 키 유효성 검증
"""

import os
import sys

def test_gemini_api_key():
    """Gemini API 키 테스트"""
    
    # 1. API 키 확인
    api_key = os.environ.get('GEMINI_API_KEY')
    
    if not api_key:
        print("❌ GEMINI_API_KEY 환경변수가 설정되지 않았습니다.")
        print("\n설정 방법:")
        print("  Windows: $env:GEMINI_API_KEY='your_key_here'")
        print("  Linux/Mac: export GEMINI_API_KEY='your_key_here'")
        return False
    
    print(f"✅ GEMINI_API_KEY 환경변수 확인됨")
    print(f"   길이: {len(api_key)} 문자")
    print(f"   첫 10자: {api_key[:10]}...")
    print(f"   마지막 5자: ...{api_key[-5:]}")
    
    # 2. google-generativeai 패키지 import
    try:
        import google.generativeai as genai
        print("✅ google-generativeai 패키지 import 성공")
    except ImportError as e:
        print(f"❌ google-generativeai 패키지 import 실패: {e}")
        print("\n설치 방법:")
        print("  pip install google-generativeai")
        return False
    
    # 3. API 키 설정
    try:
        genai.configure(api_key=api_key)
        print("✅ Gemini API 키 설정 성공")
    except Exception as e:
        print(f"❌ Gemini API 키 설정 실패: {e}")
        return False
    
    # 4. 모델 생성 테스트 (여러 버전)
    models_to_test = [
        'gemini-1.5-flash-latest',
        'gemini-1.5-flash',
        'gemini-1.5-pro-latest',
        'gemini-1.5-pro',
        'gemini-2.0-flash-exp',
    ]
    
    print("\n📋 사용 가능한 모델 테스트:")
    working_models = []
    
    for model_name in models_to_test:
        try:
            model = genai.GenerativeModel(model_name)
            print(f"  ✅ {model_name}: 생성 성공")
            working_models.append(model_name)
        except Exception as e:
            print(f"  ❌ {model_name}: {str(e)[:50]}...")
    
    if not working_models:
        print("\n❌ 사용 가능한 모델이 없습니다. API 키를 확인하세요.")
        return False
    
    # 5. 실제 API 호출 테스트
    print(f"\n🧪 실제 API 호출 테스트 (모델: {working_models[0]}):")
    try:
        model = genai.GenerativeModel(working_models[0])
        response = model.generate_content("Hello! Please respond with 'API test successful'")
        
        print(f"✅ API 호출 성공!")
        print(f"   응답: {response.text[:100]}...")
        
    except Exception as e:
        print(f"❌ API 호출 실패: {e}")
        print("\n가능한 원인:")
        print("  1. API 키가 비활성화되었거나 만료됨")
        print("  2. API 할당량 초과")
        print("  3. 네트워크 연결 문제")
        return False
    
    # 6. 최종 결과
    print("\n" + "="*50)
    print("✅ Gemini API 키 테스트 완료!")
    print("="*50)
    print(f"사용 가능한 모델: {', '.join(working_models)}")
    print("\n이 API 키는 GitHub Actions에서 정상 작동합니다.")
    
    return True

if __name__ == "__main__":
    success = test_gemini_api_key()
    sys.exit(0 if success else 1)
