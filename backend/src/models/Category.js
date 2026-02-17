const mongoose = require('mongoose');

const categorySchema = new mongoose.Schema({
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
    maxlength: 50,
  },
  type: {
    type: String,
    enum: ['income', 'expense'],
    required: true,
  },
  icon: {
    type: String,
    default: 'tag',
    maxlength: 50,
  },
  color: {
    type: String,
    default: '#8b5cf6',
    match: /^#[0-9A-Fa-f]{6}$/,
  },
  parent: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Category',
    default: null,
  },
  isSystem: {
    type: Boolean,
    default: false,
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

categorySchema.index({ user: 1, name: 1, type: 1 }, { unique: true });

categorySchema.methods.toSafeObject = function() {
  return {
    id: this._id.toString(),
    userId: this.user.toString(),
    name: this.name,
    type: this.type,
    icon: this.icon,
    color: this.color,
    parentId: this.parent ? this.parent.toString() : null,
    isSystem: this.isSystem,
    createdAt: this.createdAt,
  };
};

module.exports = mongoose.model('Category', categorySchema);
