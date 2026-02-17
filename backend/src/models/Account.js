const mongoose = require('mongoose');

const accountSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
    index: true,
  },
  name: {
    type: String,
    required: true,
    trim: true,
    maxlength: 100,
  },
  type: {
    type: String,
    enum: ['bank', 'wallet', 'cash', 'credit_card'],
    required: true,
  },
  balance: {
    type: Number,
    default: 0,
    min: 0,
  },
  institution: {
    type: String,
    trim: true,
    maxlength: 100,
  },
  accountNumber: {
    type: String,
    trim: true,
    maxlength: 255,
  },
  last4Digits: {
    type: String,
    maxlength: 4,
  },
  color: {
    type: String,
    default: '#6366f1',
    match: /^#[0-9A-Fa-f]{6}$/,
  },
  icon: {
    type: String,
    default: 'wallet',
    maxlength: 50,
  },
  isActive: {
    type: Boolean,
    default: true,
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

accountSchema.index({ user: 1, name: 1 }, { unique: true });

accountSchema.methods.toSafeObject = function() {
  return {
    id: this._id.toString(),
    userId: this.user.toString(),
    name: this.name,
    type: this.type,
    balance: this.balance,
    institution: this.institution,
    accountNumber: this.accountNumber,
    last4Digits: this.last4Digits,
    color: this.color,
    icon: this.icon,
    isActive: this.isActive,
    createdAt: this.createdAt,
    updatedAt: this.updatedAt,
  };
};

module.exports = mongoose.model('Account', accountSchema);
