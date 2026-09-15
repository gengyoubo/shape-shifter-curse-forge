package net.minecraft.world.inventory;

import com.mojang.logging.LogUtils;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class AnvilMenu extends ItemCombinerMenu {
   public static final int f_265994_ = 0;
   public static final int f_265986_ = 1;
   public static final int f_266102_ = 2;
   private static final Logger f_38999_ = LogUtils.getLogger();
   private static final boolean f_150469_ = false;
   public static final int f_150468_ = 50;
   public int f_39000_;
   @Nullable
   private String f_39001_;
   private final DataSlot f_39002_ = DataSlot.m_39401_();
   private static final int f_150470_ = 0;
   private static final int f_150471_ = 1;
   private static final int f_150472_ = 1;
   private static final int f_150464_ = 1;
   private static final int f_150465_ = 2;
   private static final int f_150466_ = 1;
   private static final int f_150467_ = 1;
   private static final int f_265898_ = 27;
   private static final int f_265878_ = 76;
   private static final int f_265992_ = 134;
   private static final int f_266013_ = 47;

   public AnvilMenu(int p_39005_, Inventory p_39006_) {
      this(p_39005_, p_39006_, ContainerLevelAccess.f_39287_);
   }

   public AnvilMenu(int p_39008_, Inventory p_39009_, ContainerLevelAccess p_39010_) {
      super(MenuType.f_39964_, p_39008_, p_39009_, p_39010_);
      this.m_38895_(this.f_39002_);
   }

   protected ItemCombinerMenuSlotDefinition m_266183_() {
      return ItemCombinerMenuSlotDefinition.m_266303_().m_266197_(0, 27, 47, (p_266635_) -> {
         return true;
      }).m_266197_(1, 76, 47, (p_266634_) -> {
         return true;
      }).m_266198_(2, 134, 47).m_266441_();
   }

   protected boolean m_8039_(BlockState p_39019_) {
      return p_39019_.m_204336_(BlockTags.f_13033_);
   }

   protected boolean m_6560_(Player p_39023_, boolean p_39024_) {
      return (p_39023_.m_150110_().f_35937_ || p_39023_.f_36078_ >= this.f_39002_.m_6501_()) && this.f_39002_.m_6501_() > 0;
   }

   protected void m_142365_(Player p_150474_, ItemStack p_150475_) {
      if (!p_150474_.m_150110_().f_35937_) {
         p_150474_.m_6749_(-this.f_39002_.m_6501_());
      }

      this.f_39769_.m_6836_(0, ItemStack.f_41583_);
      if (this.f_39000_ > 0) {
         ItemStack itemstack = this.f_39769_.m_8020_(1);
         if (!itemstack.m_41619_() && itemstack.m_41613_() > this.f_39000_) {
            itemstack.m_41774_(this.f_39000_);
            this.f_39769_.m_6836_(1, itemstack);
         } else {
            this.f_39769_.m_6836_(1, ItemStack.f_41583_);
         }
      } else {
         this.f_39769_.m_6836_(1, ItemStack.f_41583_);
      }

      this.f_39002_.m_6422_(0);
      this.f_39770_.m_39292_((p_150479_, p_150480_) -> {
         BlockState blockstate = p_150479_.m_8055_(p_150480_);
         if (!p_150474_.m_150110_().f_35937_ && blockstate.m_204336_(BlockTags.f_13033_) && p_150474_.m_217043_().m_188501_() < 0.12F) {
            BlockState blockstate1 = AnvilBlock.m_48824_(blockstate);
            if (blockstate1 == null) {
               p_150479_.m_7471_(p_150480_, false);
               p_150479_.m_46796_(1029, p_150480_, 0);
            } else {
               p_150479_.m_7731_(p_150480_, blockstate1, 2);
               p_150479_.m_46796_(1030, p_150480_, 0);
            }
         } else {
            p_150479_.m_46796_(1030, p_150480_, 0);
         }

      });
   }

   public void m_6640_() {
      ItemStack itemstack = this.f_39769_.m_8020_(0);
      this.f_39002_.m_6422_(1);
      int i = 0;
      int j = 0;
      int k = 0;
      if (itemstack.m_41619_()) {
         this.f_39768_.m_6836_(0, ItemStack.f_41583_);
         this.f_39002_.m_6422_(0);
      } else {
         ItemStack itemstack1 = itemstack.m_41777_();
         ItemStack itemstack2 = this.f_39769_.m_8020_(1);
         Map<Enchantment, Integer> map = EnchantmentHelper.m_44831_(itemstack1);
         j += itemstack.m_41610_() + (itemstack2.m_41619_() ? 0 : itemstack2.m_41610_());
         this.f_39000_ = 0;
         if (!itemstack2.m_41619_()) {
            boolean flag = itemstack2.m_150930_(Items.f_42690_) && !EnchantedBookItem.m_41163_(itemstack2).isEmpty();
            if (itemstack1.m_41763_() && itemstack1.m_41720_().m_6832_(itemstack, itemstack2)) {
               int l2 = Math.min(itemstack1.m_41773_(), itemstack1.m_41776_() / 4);
               if (l2 <= 0) {
                  this.f_39768_.m_6836_(0, ItemStack.f_41583_);
                  this.f_39002_.m_6422_(0);
                  return;
               }

               int i3;
               for(i3 = 0; l2 > 0 && i3 < itemstack2.m_41613_(); ++i3) {
                  int j3 = itemstack1.m_41773_() - l2;
                  itemstack1.m_41721_(j3);
                  ++i;
                  l2 = Math.min(itemstack1.m_41773_(), itemstack1.m_41776_() / 4);
               }

               this.f_39000_ = i3;
            } else {
               if (!flag && (!itemstack1.m_150930_(itemstack2.m_41720_()) || !itemstack1.m_41763_())) {
                  this.f_39768_.m_6836_(0, ItemStack.f_41583_);
                  this.f_39002_.m_6422_(0);
                  return;
               }

               if (itemstack1.m_41763_() && !flag) {
                  int l = itemstack.m_41776_() - itemstack.m_41773_();
                  int i1 = itemstack2.m_41776_() - itemstack2.m_41773_();
                  int j1 = i1 + itemstack1.m_41776_() * 12 / 100;
                  int k1 = l + j1;
                  int l1 = itemstack1.m_41776_() - k1;
                  if (l1 < 0) {
                     l1 = 0;
                  }

                  if (l1 < itemstack1.m_41773_()) {
                     itemstack1.m_41721_(l1);
                     i += 2;
                  }
               }

               Map<Enchantment, Integer> map1 = EnchantmentHelper.m_44831_(itemstack2);
               boolean flag2 = false;
               boolean flag3 = false;

               for(Enchantment enchantment1 : map1.keySet()) {
                  if (enchantment1 != null) {
                     int i2 = map.getOrDefault(enchantment1, 0);
                     int j2 = map1.get(enchantment1);
                     j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2);
                     boolean flag1 = enchantment1.m_6081_(itemstack);
                     if (this.f_39771_.m_150110_().f_35937_ || itemstack.m_150930_(Items.f_42690_)) {
                        flag1 = true;
                     }

                     for(Enchantment enchantment : map.keySet()) {
                        if (enchantment != enchantment1 && !enchantment1.m_44695_(enchantment)) {
                           flag1 = false;
                           ++i;
                        }
                     }

                     if (!flag1) {
                        flag3 = true;
                     } else {
                        flag2 = true;
                        if (j2 > enchantment1.m_6586_()) {
                           j2 = enchantment1.m_6586_();
                        }

                        map.put(enchantment1, j2);
                        int k3 = 0;
                        switch (enchantment1.m_44699_()) {
                           case COMMON:
                              k3 = 1;
                              break;
                           case UNCOMMON:
                              k3 = 2;
                              break;
                           case RARE:
                              k3 = 4;
                              break;
                           case VERY_RARE:
                              k3 = 8;
                        }

                        if (flag) {
                           k3 = Math.max(1, k3 / 2);
                        }

                        i += k3 * j2;
                        if (itemstack.m_41613_() > 1) {
                           i = 40;
                        }
                     }
                  }
               }

               if (flag3 && !flag2) {
                  this.f_39768_.m_6836_(0, ItemStack.f_41583_);
                  this.f_39002_.m_6422_(0);
                  return;
               }
            }
         }

         if (this.f_39001_ != null && !Util.m_288217_(this.f_39001_)) {
            if (!this.f_39001_.equals(itemstack.m_41786_().getString())) {
               k = 1;
               i += k;
               itemstack1.m_41714_(Component.m_237113_(this.f_39001_));
            }
         } else if (itemstack.m_41788_()) {
            k = 1;
            i += k;
            itemstack1.m_41787_();
         }

         this.f_39002_.m_6422_(j + i);
         if (i <= 0) {
            itemstack1 = ItemStack.f_41583_;
         }

         if (k == i && k > 0 && this.f_39002_.m_6501_() >= 40) {
            this.f_39002_.m_6422_(39);
         }

         if (this.f_39002_.m_6501_() >= 40 && !this.f_39771_.m_150110_().f_35937_) {
            itemstack1 = ItemStack.f_41583_;
         }

         if (!itemstack1.m_41619_()) {
            int k2 = itemstack1.m_41610_();
            if (!itemstack2.m_41619_() && k2 < itemstack2.m_41610_()) {
               k2 = itemstack2.m_41610_();
            }

            if (k != i || k == 0) {
               k2 = m_39025_(k2);
            }

            itemstack1.m_41742_(k2);
            EnchantmentHelper.m_44865_(map, itemstack1);
         }

         this.f_39768_.m_6836_(0, itemstack1);
         this.m_38946_();
      }
   }

   public static int m_39025_(int p_39026_) {
      return p_39026_ * 2 + 1;
   }

   public boolean m_39020_(String p_288970_) {
      String s = m_288226_(p_288970_);
      if (s != null && !s.equals(this.f_39001_)) {
         this.f_39001_ = s;
         if (this.m_38853_(2).m_6657_()) {
            ItemStack itemstack = this.m_38853_(2).m_7993_();
            if (Util.m_288217_(s)) {
               itemstack.m_41787_();
            } else {
               itemstack.m_41714_(Component.m_237113_(s));
            }
         }

         this.m_6640_();
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   private static String m_288226_(String p_288995_) {
      String s = SharedConstants.m_136190_(p_288995_);
      return s.length() <= 50 ? s : null;
   }

   public int m_39028_() {
      return this.f_39002_.m_6501_();
   }
}
