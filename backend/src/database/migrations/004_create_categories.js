/**
 * Create categories table
 */
exports.up = function(knex) {
  return knex.schema.createTable('categories', (table) => {
    table.uuid('id').primary().defaultTo(knex.raw('gen_random_uuid()'));
    table.uuid('user_id').references('id').inTable('users').onDelete('CASCADE');
    table.string('name', 50).notNullable();
    table.enu('type', ['income', 'expense']).notNullable();
    table.string('icon', 50).defaultTo('tag');
    table.string('color', 7).defaultTo('#8b5cf6');
    table.uuid('parent_id').references('id').inTable('categories');
    table.boolean('is_system').defaultTo(false);
    table.timestamp('created_at').defaultTo(knex.fn.now());

    table.unique(['user_id', 'name', 'type']);
    table.index('user_id');
  });
};

exports.down = function(knex) {
  return knex.schema.dropTableIfExists('categories');
};
