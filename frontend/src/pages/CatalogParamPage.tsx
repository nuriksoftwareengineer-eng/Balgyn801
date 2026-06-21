import { useParams } from "react-router-dom";
import { ProductPage } from "@/pages/ProductPage";
import { GroupPage } from "@/pages/GroupPage";

/**
 * Dispatcher for /catalog/:param.
 * Legacy product URLs use numeric IDs (e.g. /catalog/42).
 * New catalog group URLs use slugs (e.g. /catalog/streetwear).
 */
export function CatalogParamPage() {
  const { param } = useParams<{ param: string }>();
  if (param && /^\d+$/.test(param)) {
    return <ProductPage />;
  }
  return <GroupPage />;
}
