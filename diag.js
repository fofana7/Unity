// Script de diagnostic - à exécuter dans le dossier backend avec: node diag.js
const { Pool } = require('pg');
const pool = new Pool({
    user: 'postgres',
    host: 'localhost',
    database: 'MiniRéseau',
    password: 'talida',
    port: 5432,
});

async function diagnose() {
    try {
        // 1. Vérifier les utilisateurs
        const users = await pool.query('SELECT id, username, email FROM users LIMIT 5');
        console.log('\n=== UTILISATEURS ===');
        console.table(users.rows);

        if (users.rows.length === 0) {
            console.log('Aucun utilisateur trouvé!');
            process.exit(1);
        }

        const userId = users.rows[0].id;
        console.log(`\n=== DIAGNOSTIQUE POUR USER ID: ${userId} (${users.rows[0].username}) ===`);

        // 2. Vérifier les posts
        const posts = await pool.query('SELECT COUNT(*) FROM posts WHERE user_id = $1', [userId]);
        console.log(`Posts: ${posts.rows[0].count}`);

        // 3. Vérifier les amitiés
        const friends = await pool.query(`
            SELECT COUNT(*) FROM friendships 
            WHERE (user_id_1 = $1 OR user_id_2 = $1) AND status = 'accepted'
        `, [userId]);
        console.log(`Amis (accepted): ${friends.rows[0].count}`);

        // 4. Vérifier toutes les amitiés (tous statuts)
        const allFriends = await pool.query(`
            SELECT * FROM friendships 
            WHERE user_id_1 = $1 OR user_id_2 = $1
        `, [userId]);
        console.log('\n=== TOUTES LES AMITIÉS ===');
        console.table(allFriends.rows);

        // 5. Tester la requête exacte du getMe
        const getMe = await pool.query(`
            SELECT id, username, email, bio,
            (SELECT COUNT(*)::int FROM friendships f WHERE (f.user_id_1 = users.id OR f.user_id_2 = users.id) AND f.status = 'accepted') as friends_count,
            (SELECT COUNT(*)::int FROM posts p WHERE p.user_id = users.id) as posts_count
            FROM users WHERE id = $1
        `, [userId]);
        console.log('\n=== RÉSULTAT getMe ===');
        console.table(getMe.rows);

        // 6. Vérifier les groupes
        const groups = await pool.query('SELECT * FROM groups LIMIT 5');
        console.log('\n=== GROUPES ===');
        console.table(groups.rows);

        const groupMembers = await pool.query('SELECT * FROM group_members LIMIT 10');
        console.log('\n=== MEMBRES DE GROUPES ===');
        console.table(groupMembers.rows);

    } catch (err) {
        console.error('Erreur:', err.message);
    } finally {
        await pool.end();
    }
}

diagnose();
