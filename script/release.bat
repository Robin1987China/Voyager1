
@echo off

@REM ���ٴ����Ŀ�ű�

@REM ����ǰ��
call cd ../ && cd web-vue && npm i && npm run build

@REM ���� Java
call cd ../ && mvn clean package
