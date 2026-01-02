/**
 * Create monthly_summaries table for cached analytics
 */
exports.up = function(knex) {
  return knex.schema.createTable('monthly_summaries', (table) => {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('user_id').references('id').inTable('users').onDelete('CASCADE');
    table.integer('year').notNullable();
    table.integer('month').notNullable();
    table.decimal('total_income', 15, 2).defaultTo(0);
    table.decimal('total_expense', 15, 2).defaultTo(0);
    table.decimal('net_savings', 15, 2).defaultTo(0);
    table.jsonb('category_breakdown');
    table.jsonb('account_breakdown');
    table.timestamp('computed_at').defaultTo(knex.fn.now());

    table.unique(['user_id', 'year', 'month']);
    table.index('user_id');
  });
};

exports.down = function(knex) {
  return knex.schema.dropTableIfExists('monthly_summaries');
};
