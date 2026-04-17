insert into members (name, created_at, updated_at)
select
    'member-' || g,
    now() - (121 - g) * interval '1 second',
    now() - (121 - g) * interval '1 second'
from generate_series(1, 120) as g;

insert into coupons (name, total_quantity, issued_quantity, issue_start_date, issue_end_date, created_at)
values ('coupon-100', 100, 0, now() - interval '1 second', now() + interval '3 days', now());
