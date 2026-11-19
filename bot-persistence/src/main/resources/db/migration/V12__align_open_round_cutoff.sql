update lunch_rounds
set draw_at = (((draw_at at time zone 'Asia/Seoul')::date + lunch_start_time
                - interval '30 minutes') at time zone 'Asia/Seoul')
where status = 'OPEN'
  and lunch_start_time is not null
  and draw_at > now();
