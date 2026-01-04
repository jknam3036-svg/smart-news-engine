@echo off
REM 최종 확인 및 Push 스크립트
REM main.py의 gemini-pro 변경사항을 GitHub에 업로드

echo ========================================
echo 최종 확인 및 GitHub Push
echo ========================================
echo.

echo [확인] 현재 main.py 상태:
echo.
findstr /N "GenerativeModel" news_crawler\main.py
echo.

echo 위에 "gemini-pro" 또는 "gemini-1.5-flash"가 보여야 합니다.
echo "gemini-1.5-flash-latest"가 보이면 문제입니다!
echo.
pause

echo.
echo [1/4] Git 상태 확인...
git status
echo.

echo [2/4] 변경사항 추가...
git add news_crawler/main.py
git add news_crawler/ecos_crawler.py
git add .github/workflows/news_update.yml
echo.

echo [3/4] 커밋...
git commit -m "Final fix: Update Gemini model to gemini-pro with fallback"
echo.

echo [4/4] Push...
git push
echo.

echo ========================================
echo 완료!
echo ========================================
echo.
echo 다음 단계:
echo 1. GitHub Actions → Run workflow
echo 2. 로그 확인:
echo    - "gemini-pro" 메시지 확인
echo    - "Phase 3: Economic Indicators" 확인
echo 3. 15분 후 Firestore 확인
echo.
pause
