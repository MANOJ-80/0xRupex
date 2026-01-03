/**
 * Fix schema issues:
 * 1. Add 'revoked' column to refresh_tokens
 * 2. Add 'notification' and 'upi' to transactions source enum
 */
exports.up = async function(knex) {
  // 1. Add revoked column to refresh_tokens
  const hasRevoked = await knex.schema.hasColumn('refresh_tokens', 'revoked');
  if (!hasRevoked) {
    await knex.schema.alterTable('refresh_tokens', (table) => {
      table.boolean('revoked').defaultTo(false);
    });
  }

  // 2. Update source enum to include notification and upi
  // First, drop the constraint
  await knex.raw(`
    ALTER TABLE transactions 
    DROP CONSTRAINT IF EXISTS transactions_source_check
  `);

  // Add new constraint with all valid sources
  await knex.raw(`
    ALTER TABLE transactions 
    ADD CONSTRAINT transactions_source_check 
    CHECK (source IN ('manual', 'sms', 'api', 'recurring', 'notification', 'upi'))
  `);

  return Promise.resolve();
};

exports.down = async function(knex) {
  // Remove revoked column
  const hasRevoked = await knex.schema.hasColumn('refresh_tokens', 'revoked');
  if (hasRevoked) {
    await knex.schema.alterTable('refresh_tokens', (table) => {
      table.dropColumn('revoked');
    });
  }

  // Revert source constraint
  await knex.raw(`
    ALTER TABLE transactions 
    DROP CONSTRAINT IF EXISTS transactions_source_check
  `);

  await knex.raw(`
    ALTER TABLE transactions 
    ADD CONSTRAINT transactions_source_check 
    CHECK (source IN ('manual', 'sms', 'api', 'recurring'))
  `);

  return Promise.resolve();
};
