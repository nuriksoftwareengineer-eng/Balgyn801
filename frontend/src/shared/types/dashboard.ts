export type DailyStat = {
  date: string;
  revenue: number;
  orders: number;
};

export type DesignStat = {
  name: string;
  count: number;
  revenue: number;
};

export type StatusStat = {
  status: string;
  count: number;
};

export type DashboardStatsResponse = {
  totalOrders: number;
  ordersToday: number;
  revenueTotal: number;
  revenueToday: number;
  totalUsers: number;
  usersToday: number;
  avgOrderValue: number;
  dailyRevenue: DailyStat[];
  topDesigns: DesignStat[];
  ordersByStatus: StatusStat[];
};
