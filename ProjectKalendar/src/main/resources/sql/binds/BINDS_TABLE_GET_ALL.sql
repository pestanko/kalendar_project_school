SELECT *
FROM
  (SELECT EVENT_ID AS eid, USER_ID AS uid, BIND_TYPE as b_type FROM BINDS) AS eres
  INNER JOIN
  (SELECT ID AS e_id, EVENT_NAME as e_EVENT_NAME, DESCRIPTION AS E_DESCRIPTION,
          ADDRESS AS  E_ADDRESS, DATE_BEGIN AS E_DATE_BEGIN, DATE_END AS E_DATE_END FROM EVENTS) AS evt
    ON eid = e_ID
  INNER JOIN
  (SELECT id AS u_ID,USER_NAME AS U_USER_NAME,FIRST_NAME AS U_FIRST_NAME,LAST_NAME AS U_LAST_NAME ,
          MOBILE_NUMBER AS U_MOBILE_NUMBER ,EMAIL AS U_EMAIL,ADDRESS AS U_ADDRESS FROM USERS) AS usr
    ON uid = u_ID