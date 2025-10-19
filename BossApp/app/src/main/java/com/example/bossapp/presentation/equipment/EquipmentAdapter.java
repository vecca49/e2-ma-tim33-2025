package com.example.bossapp.presentation.equipment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bossapp.R;
import com.example.bossapp.data.model.Equipment;

import java.util.ArrayList;
import java.util.List;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.EquipmentViewHolder> {

    private List<Equipment> equipmentList;
    private final OnEquipmentActionListener listener;
    private final boolean showPrice;
    private final int userLevel;

    public interface OnEquipmentActionListener {
        void onBuyClick(Equipment equipment, int price);
        void onActivateClick(Equipment equipment);
        void onDeactivateClick(Equipment equipment);
        void onUpgradeClick(Equipment equipment, int upgradeCost);
    }

    public EquipmentAdapter(OnEquipmentActionListener listener, boolean showPrice, int userLevel) {
        this.equipmentList = new ArrayList<>();
        this.listener = listener;
        this.showPrice = showPrice;
        this.userLevel = userLevel;
    }

    public void setEquipmentList(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    class EquipmentViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvDescription;
        private final TextView tvPrice;
        private final TextView tvQuantity;
        private final TextView tvDuration;
        private final Button btnAction;
        private final Button btnUpgrade;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEquipmentName);
            tvDescription = itemView.findViewById(R.id.tvEquipmentDescription);
            tvPrice = itemView.findViewById(R.id.tvEquipmentPrice);
            tvQuantity = itemView.findViewById(R.id.tvEquipmentQuantity);
            tvDuration = itemView.findViewById(R.id.tvEquipmentDuration);
            btnAction = itemView.findViewById(R.id.btnEquipmentAction);
            btnUpgrade = itemView.findViewById(R.id.btnEquipmentUpgrade);
        }

        public void bind(Equipment equipment) {
            tvName.setText(equipment.getDisplayName());

            // Description by type
            String description = getEquipmentDescription(equipment);
            tvDescription.setText(description);

            // Price display (only in the Shop)
            if (showPrice) {
                int price = Equipment.calculatePrice(equipment.getType(),
                        equipment.getSubType(), userLevel);
                tvPrice.setText("Price: " + price + " coins");
                tvPrice.setVisibility(View.VISIBLE);

                btnAction.setText("Buy");
                btnAction.setOnClickListener(v -> listener.onBuyClick(equipment, price));

                tvQuantity.setVisibility(View.GONE);
                tvDuration.setVisibility(View.GONE);
                btnUpgrade.setVisibility(View.GONE);

            } else {
                // Inventar mode
                tvPrice.setVisibility(View.GONE);

                // Quantity (only for potione)
                if (equipment.getType() == Equipment.EquipmentType.POTION) {
                    tvQuantity.setText("Qty: " + equipment.getQuantity());
                    tvQuantity.setVisibility(View.VISIBLE);
                } else {
                    tvQuantity.setVisibility(View.GONE);
                }

                // Duration (for armor and temp potions)
                if (equipment.getRemainingDuration() > 0) {
                    tvDuration.setText("Duration: " + equipment.getRemainingDuration() +
                            (equipment.getType() == Equipment.EquipmentType.ARMOR ? " fights" : " fight"));
                    tvDuration.setVisibility(View.VISIBLE);
                } else if (equipment.getRemainingDuration() == -1) {
                    tvDuration.setText("Permanent");
                    tvDuration.setVisibility(View.VISIBLE);
                } else {
                    tvDuration.setVisibility(View.GONE);
                }

                // Activate/Deactivate button
                if (equipment.getIsActive()) {  // ← KORISTI getIsActive()
                    btnAction.setText("Deactivate");
                    btnAction.setOnClickListener(v -> listener.onDeactivateClick(equipment));
                } else {
                    btnAction.setText("Activate");
                    btnAction.setOnClickListener(v -> listener.onActivateClick(equipment));
                }

                // Upgrade button (only for weapons)
                if (equipment.getType() == Equipment.EquipmentType.WEAPON) {
                    int upgradeCost = Equipment.calculatePrice(Equipment.EquipmentType.WEAPON,
                            equipment.getSubType(), userLevel);
                    btnUpgrade.setText("Upgrade (" + upgradeCost + " coins)");
                    btnUpgrade.setVisibility(View.VISIBLE);
                    btnUpgrade.setOnClickListener(v -> listener.onUpgradeClick(equipment, upgradeCost));
                } else {
                    btnUpgrade.setVisibility(View.GONE);
                }
            }
        }

        private String getEquipmentDescription(Equipment equipment) {
            switch (equipment.getType()) {
                case POTION:
                    Equipment.PotionType potion = Equipment.PotionType.valueOf(equipment.getSubType());
                    return "+" + (int)(potion.powerBoost * 100) + "% Power" +
                            (potion.isTemporary ? " (1 battle)" : " (Permanent)");

                case ARMOR:
                    Equipment.ArmorType armor = Equipment.ArmorType.valueOf(equipment.getSubType());
                    String bonus = "";
                    switch (armor.bonusType) {
                        case "powerBoost":
                            bonus = "+" + (int)(armor.value * 100) + "% Power";
                            break;
                        case "successRate":
                            bonus = "+" + (int)(armor.value * 100) + "% Success Rate";
                            break;
                        case "extraAttack":
                            bonus = (int)(armor.value * 100) + "% chance for +1 attack";
                            break;
                    }
                    return bonus + " (2 fights)";

                case WEAPON:
                    Equipment.WeaponType weapon = Equipment.WeaponType.valueOf(equipment.getSubType());
                    String weaponBonus = "";
                    if (weapon.bonusType.equals("powerBoost")) {
                        weaponBonus = "+" + String.format("%.2f", equipment.getCurrentValue() * 100) + "% Power";
                    } else {
                        weaponBonus = "+" + String.format("%.2f", equipment.getCurrentValue() * 100) + "% Coin Reward";
                    }
                    return weaponBonus + " (Permanent)";
            }
            return "";
        }
    }
}