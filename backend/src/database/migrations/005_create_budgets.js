/**
 * Create budgets table
 */
exports.up = function(knex) {
  return knex.schema.createTable('budgets', (table) => {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('user_id').references('id').inTable('users').onDelete('CASCADE');
    table.uuid('category_id').references('id').inTable('categories').onDelete('CASCADE');
    table.decimal('amount', 15, 2).notNullable();
    table.string('period', 20).defaultTo('monthly');
    table.date('start_date').notNullable();
    table.date('end_date');
    table.decimal('alert_threshold', 3, 2).defaultTo(0.80);
    table.boolean('is_active').defaultTo(true);
    table.timestamp('created_at').defaultTo(knex.fn.now());
    table.timestamp('updated_at').defaultTo(knex.fn.now());

    table.index('user_id');
    table.index(['user_id', 'is_active']);
  });
};

exports.down = function(knex) {
  return knex.schema.dropTableIfExists('budgets');
};
