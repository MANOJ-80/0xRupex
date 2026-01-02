/**
 * Create accounts table
 */
exports.up = function(knex) {
  return knex.schema.createTable('accounts', (table) => {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('user_id').references('id').inTable('users').onDelete('CASCADE');
    table.string('name', 100).notNullable();
    table.enu('type', ['bank', 'wallet', 'cash', 'credit_card']).notNullable();
    table.decimal('balance', 15, 2).defaultTo(0);
    table.string('institution', 100);
    table.string('account_number', 255); // Encrypted
    table.string('last_4_digits', 4);
    table.string('color', 7).defaultTo('#6366f1');
    table.string('icon', 50).defaultTo('wallet');
    table.boolean('is_active').defaultTo(true);
    table.timestamp('created_at').defaultTo(knex.fn.now());
    table.timestamp('updated_at').defaultTo(knex.fn.now());

    table.unique(['user_id', 'name']);
    table.index('user_id');
  });
};

exports.down = function(knex) {
  return knex.schema.dropTableIfExists('accounts');
};
