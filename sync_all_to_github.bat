@echo off
REM GitHub 전체 폴더 덮어쓰기 스크립트
REM 안전한 방법으로 모든 파일 업로드

echo ========================================
echo GitHub 전체 폴더 덮어쓰기
echo ========================================
echo.

echo 현재 위치: %CD%
echo.

REM 1. Git 상태 확인
echo [1/6] Git 상태 확인 중...
git status
echo.

REM 2. 중복 폴더 삭제
echo [2/6] 중복 폴더 삭제 중...
if exist "news_crawler\.github" (
    git rm -r news_crawler/.github
    echo 중복 폴더 삭제 완료
) else (
    echo 중복 폴더 없음
)
echo.

REM 3. 모든 파일 추가 (추적되지 않은 파일 포함)
echo [3/6] 모든 파일 추가 중...
git add -A
echo.

REM 4. 변경사항 확인
echo [4/6] 변경사항 확인...
git status
echo.
echo 위 파일들이 업로드됩니다.
echo.
pause

REM 5. 커밋
echo [5/6] 커밋 중...
git commit -m "Complete sync: Upload all files from local to GitHub"
if errorlevel 1 (
    echo.
    echo 변경사항이 없거나 이미 커밋되었습니다.
    echo.
) else (
    echo 커밋 완료
    echo.
)

REM 6. Push
echo [6/6] GitHub에 업로드 중...
git push origin main
if errorlevel 1 (
    echo.
    echo Push 실패. 다시 시도하거나 수동으로 실행하세요:
    echo git push origin main --force
    echo.
) else (
    echo.
    echo ========================================
    echo 업로드 완료!
    echo ========================================
    echo.
)

echo 다음 단계:
echo 1. GitHub Repository 새로고침
echo 2. 파일 구조 확인
echo 3. Actions 탭에서 "Run workflow" 클릭
echo.
pause
