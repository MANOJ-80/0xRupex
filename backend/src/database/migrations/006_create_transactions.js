/**
 * Create transactions table
 */
exports.up = function(knex) {
  return knex.schema.createTable('transactions', (table) => {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('user_id').references('id').inTable('users').onDelete('CASCADE');
    table.uuid('account_id').references('id').inTable('accounts').onDelete('SET NULL');
    table.uuid('category_id').references('id').inTable('categories').onDelete('SET NULL');
    table.enu('type', ['income', 'expense', 'transfer']).notNullable();
    table.decimal('amount', 15, 2).notNullable();
    table.string('description', 255);
    table.string('merchant', 100);
    table.string('reference_id', 100);
    table.enu('source', ['manual', 'sms', 'api', 'recurring']).defaultTo('manual');
    table.timestamp('transaction_at').notNullable();
    table.string('location', 255);
    table.specificType('tags', 'text[]');
    table.text('notes');
    table.boolean('is_recurring').defaultTo(false);
    table.string('sms_hash', 64).unique();
    table.timestamp('created_at').defaultTo(knex.fn.now());
    table.timestamp('updated_at').defaultTo(knex.fn.now());

    table.index(['user_id', 'transaction_at']);
    table.index(['user_id', 'category_id']);
    table.index(['user_id', 'account_id']);
    table.index('sms_hash');
  });
};

exports.down = function(knex) {
  return knex.schema.dropTableIfExists('transactions');
};
