import React, { Suspense } from "react";
import { createRoot } from "react-dom/client";

const Frontend1 = React.lazy(() => import("frontend1/App"));
const Frontend2 = React.lazy(() => import("frontend2/App"));

createRoot(document.getElementById("root")).render(
  <Suspense fallback="Loading">
    <Frontend1 />
    <Frontend2 />
  </Suspense>
);
