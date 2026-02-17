const mongoose = require('mongoose');

const transactionSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true,
  },
  account: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Account',
    default: null,
  },
  category: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Category',
    default: null,
  },
  type: {
    type: String,
    enum: ['income', 'expense', 'transfer'],
    required: true,
  },
  amount: {
    type: Number,
    required: true,
    min: 0.01,
  },
  description: {
    type: String,
    trim: true,
    maxlength: 255,
  },
  merchant: {
    type: String,
    trim: true,
    maxlength: 100,
  },
  referenceId: {
    type: String,
    trim: true,
    maxlength: 100,
  },
  source: {
    type: String,
    enum: ['manual', 'sms', 'api', 'recurring'],
    default: 'manual',
  },
  transactionAt: {
    type: Date,
    required: true,
    default: Date.now,
  },
  location: {
    type: String,
    trim: true,
    maxlength: 255,
  },
  tags: [{
    type: String,
    trim: true,
  }],
  notes: {
    type: String,
    trim: true,
  },
  isRecurring: {
    type: Boolean,
    default: false,
  },
  smsHash: {
    type: String,
    unique: true,
    sparse: true,
    maxlength: 64,
  },
}, {
  timestamps: true,
  toJSON: {
    virtuals: true,
    transform: function(doc, ret) {
      ret.id = ret._id.toString();
      delete ret._id;
      delete ret.__v;
      return ret;
    },
  },
});

transactionSchema.index({ user: 1, transactionAt: -1 });
transactionSchema.index({ user: 1, category: 1 });
transactionSchema.index({ user: 1, account: 1 });

transactionSchema.methods.toSafeObject = function() {
  return {
    id: this._id.toString(),
    userId: this.user ? this.user.toString() : null,
    accountId: this.account && typeof this.account === 'object' 
      ? this.account._id?.toString() || null 
      : this.account?.toString() || null,
    categoryId: this.category && typeof this.category === 'object' 
      ? this.category._id?.toString() || null 
      : this.category?.toString() || null,
    type: this.type,
    amount: this.amount,
    description: this.description,
    merchant: this.merchant,
    referenceId: this.referenceId,
    source: this.source,
    transactionAt: this.transactionAt,
    location: this.location,
    tags: this.tags,
    notes: this.notes,
    isRecurring: this.isRecurring,
    smsHash: this.smsHash,
    createdAt: this.createdAt,
    updatedAt: this.updatedAt,
  };
};

transactionSchema.methods.toPopulatedObject = function() {
  const obj = this.toSafeObject();
  if (this.populated('category')) {
    obj.categoryName = this.category?.name || null;
    obj.categoryIcon = this.category?.icon || null;
    obj.categoryColor = this.category?.color || null;
  }
  if (this.populated('account')) {
    obj.accountName = this.account?.name || null;
  }
  return obj;
};

module.exports = mongoose.model('Transaction', transactionSchema);
