/**
 * Create refresh_tokens table
 */
exports.up = function(knex) {
  return knex.schema.createTable('refresh_tokens', (table) => {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('user_id').references('id').inTable('users').onDelete('CASCADE');
    table.string('token_hash', 255).notNullable();
    table.string('device_info', 255);
    table.timestamp('expires_at').notNullable();
    table.timestamp('created_at').defaultTo(knex.fn.now());

    table.index('user_id');
    table.index('token_hash');
  });
};

exports.down = function(knex) {
  return knex.schema.dropTableIfExists('refresh_tokens');
};
