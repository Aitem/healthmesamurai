(ns app.routes)

(def routes
  {:.          :app.pages.index.model/index-page
   :layout     :main
   "game"      {:. :app.pages.game.model/index-page
                :layout     :main}})