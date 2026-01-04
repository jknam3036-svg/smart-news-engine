@echo off
REM 최종 수정사항 GitHub 업로드
REM Gemini 모델을 gemini-pro로 변경

echo ========================================
echo 최종 수정사항 GitHub 업로드
echo Gemini 모델: gemini-pro (안정적)
echo ========================================
echo.

REM 1. 변경사항 추가
echo [1/4] 변경사항 추가 중...
git add news_crawler/main.py
echo.

REM 2. 상태 확인
echo [2/4] 변경사항 확인...
git status
echo.

REM 3. 커밋
echo [3/4] 커밋 중...
git commit -m "Fix: Change Gemini model to 'gemini-pro' for better stability"
echo.

REM 4. Push
echo [4/4] GitHub에 업로드 중...
git push
echo.

echo ========================================
echo 완료!
echo ========================================
echo.
echo 다음 단계:
echo 1. GitHub Actions 탭에서 "Run workflow" 클릭
echo 2. 로그 확인:
echo    - "gemini-pro" 또는 "gemini-1.5-flash" 메시지
echo    - "Batch analyzed successfully" 메시지
echo 3. Firestore에서 한국어 데이터 확인
echo.
pause
