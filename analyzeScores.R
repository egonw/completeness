
scores = read.csv2(
  "/home/egonw/bioclipse-workspace/D5.6 - Completeness/foo.csv"
)

png("/home/egonw/bioclipse-workspace/D5.6 - Completeness/foo_hist.png")
hist(
  as.numeric(scores[,"score"]),
  xlab="PhysChem score", main="",
  nclass=4
)
dev.off()
