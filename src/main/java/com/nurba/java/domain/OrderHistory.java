package com.nurba.java.domain;


import com.nurba.java.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "order_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistory  {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_history_seq")
    @SequenceGenerator(
            name = "order_history_seq",
            sequenceName = "ORDER_STATUS_HIST_ID_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(targetEntity = Order.class)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    private OrderStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_ADDED", nullable = false)
    private Date dateAdded;

}
