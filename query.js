const { pool } = require('c:/Users/user/backend/config/db');
pool.query("SELECT id, (SELECT COUNT(*) FROM friendships f WHERE (f.user_id_1 = users.id OR f.user_id_2 = users.id) AND f.status = 'accepted') as friends_count FROM users").then(res => {
    console.log(res.rows);
    process.exit(0);
}).catch(console.error);
