package vCampus.client;

import javax.swing.*;
import java.awt.*;
import vCampus.common.shop.Product;

public class ProductCard extends JPanel {
    private JLabel lblImage;
    private JLabel lblName;
    private JLabel lblPrice;
    private JLabel lblStock;
    private JButton btnAddToCart;

    public ProductCard(Product product, ImageIcon imageIcon) {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true));
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(180, 220));
        setMaximumSize(new Dimension(180, 220));

        // 图片（固定大小）
        lblImage = new JLabel();
        if (imageIcon != null) {
            Image scaled = imageIcon.getImage().getScaledInstance(160, 120, Image.SCALE_SMOOTH);
            lblImage.setIcon(new ImageIcon(scaled));
        } else {
            lblImage.setText("🖼️");
            lblImage.setHorizontalAlignment(SwingConstants.CENTER);
            lblImage.setPreferredSize(new Dimension(160, 120));
        }
        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        add(lblImage, BorderLayout.CENTER);

        // 信息面板（底部）
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 2, 2));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

        lblName = new JLabel(product.getName());
        lblName.setFont(new Font("微软雅黑", Font.BOLD, 13));
        lblName.setForeground(new Color(50, 50, 50));

        lblPrice = new JLabel("¥ " + String.format("%.2f", product.getPrice()));
        lblPrice.setFont(new Font("微软雅黑", Font.BOLD, 14));
        lblPrice.setForeground(new Color(220, 50, 50));

        lblStock = new JLabel("库存: " + product.getStock());
        lblStock.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        lblStock.setForeground(new Color(150, 150, 150));

        infoPanel.add(lblName);
        infoPanel.add(lblPrice);
        infoPanel.add(lblStock);
        add(infoPanel, BorderLayout.SOUTH);

        // 鼠标悬停效果
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                setBorder(BorderFactory.createLineBorder(new Color(52, 152, 219), 2, true));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true));
            }
        });
    }

    public JLabel getImageLabel() { return lblImage; }
    public JButton getBtnAddToCart() { return btnAddToCart; }
}