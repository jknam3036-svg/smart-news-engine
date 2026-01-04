@echo off
REM GitHub 소스파일 전체 업로드 스크립트
REM 실행 방법: 이 파일을 프로젝트 루트에 저장하고 더블클릭

echo ========================================
echo GitHub 소스파일 전체 업로드
echo ========================================
echo.

REM 1. 중복 워크플로우 폴더 삭제
echo [1/5] 중복 파일 삭제 중...
git rm -r news_crawler/.github
if errorlevel 1 (
    echo 중복 파일이 이미 삭제되었거나 없습니다.
)
echo.

REM 2. 모든 변경사항 추가
echo [2/5] 모든 파일 추가 중...
git add .
echo.

REM 3. 상태 확인
echo [3/5] Git 상태 확인...
git status
echo.

REM 4. 커밋
echo [4/5] 커밋 중...
git commit -m "Complete update: Add ecos_crawler, fix Gemini model, cleanup duplicates"
echo.

REM 5. Push
echo [5/5] GitHub에 업로드 중...
git push
echo.

echo ========================================
echo 완료!
echo ========================================
echo.
echo 다음 단계:
echo 1. GitHub Repository 새로고침
echo 2. Actions 탭에서 "Run workflow" 클릭
echo 3. 로그 확인
echo.
pause
