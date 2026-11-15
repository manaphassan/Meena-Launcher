# Image helper for store assets (Windows PowerShell, System.Drawing; no extra installs).
#   img.ps1 -In a.png -Out b.png -Width 512 -Height 512            resize to 32-bit PNG with alpha
#   img.ps1 -In a.png -Out b.png -Width 1080 -Height 1920 -Rgb     resize to 24-bit PNG, no alpha
#   add -CropX/-CropY/-CropW/-CropH to take a part of the source first
#   img.ps1 -In a.png -Info                                         print size and pixel format
param([string]$In, [string]$Out, [int]$Width, [int]$Height,
      [int]$CropX = 0, [int]$CropY = 0, [int]$CropW = 0, [int]$CropH = 0, [switch]$Rgb, [switch]$Info)
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile((Resolve-Path $In))
if ($Info) { "{0}x{1} {2}" -f $src.Width, $src.Height, $src.PixelFormat; $src.Dispose(); exit }
if ($CropW -eq 0) { $CropW = $src.Width - $CropX }
if ($CropH -eq 0) { $CropH = $src.Height - $CropY }
$fmt = if ($Rgb) { [System.Drawing.Imaging.PixelFormat]::Format24bppRgb } else { [System.Drawing.Imaging.PixelFormat]::Format32bppArgb }
$dst = New-Object System.Drawing.Bitmap $Width, $Height, $fmt
$g = [System.Drawing.Graphics]::FromImage($dst)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
if ($Rgb) { $g.Clear([System.Drawing.Color]::White) } else { $g.Clear([System.Drawing.Color]::Transparent) }
$attr = New-Object System.Drawing.Imaging.ImageAttributes
$attr.SetWrapMode([System.Drawing.Drawing2D.WrapMode]::TileFlipXY)
$g.DrawImage($src, (New-Object System.Drawing.Rectangle 0, 0, $Width, $Height), $CropX, $CropY, $CropW, $CropH, [System.Drawing.GraphicsUnit]::Pixel, $attr)
$g.Dispose(); $src.Dispose()
$dst.Save($ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Out), [System.Drawing.Imaging.ImageFormat]::Png)
$dst.Dispose()
"{0} {1}x{2} {3}" -f $Out, $Width, $Height, $(if ($Rgb) { "24-bit" } else { "32-bit" })
