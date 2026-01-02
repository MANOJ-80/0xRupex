/**
 * Create users table
 */
exports.up = function(knex) {
  return knex.schema.createTable('users', (table) => {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.string('email', 255).unique().notNullable();
    table.string('password_hash', 255).notNullable();
    table.string('name', 100).notNullable();
    table.string('phone', 15);
    table.string('currency', 3).defaultTo('INR');
    table.string('timezone', 50).defaultTo('Asia/Kolkata');
    table.timestamp('created_at').defaultTo(knex.fn.now());
    table.timestamp('updated_at').defaultTo(knex.fn.now());
    table.timestamp('last_login');
    table.boolean('is_active').defaultTo(true);
  });
};

exports.down = function(knex) {
  return knex.schema.dropTableIfExists('users');
};
