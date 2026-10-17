# Régénère assets/rolenametag/textures/font/icons.png (4 glyphes de 20x20 : caméra, +, losange, étoile)
from PIL import Image
CAM = ["..............MM","....MMMMMMMMMMMM","MM..MMM.....MMMM","MM..MMM.....MMMM","MMMMMMMMMMMMMMMM",
       "MMMMMMMMMMMMMMMM","MMMMMMMMMMMMMMMM","MM.MMMMMMMMMMMMM","MM.MMMMMMMMMMMMM"]
PLUS = ["....MM....","....MM....","....MM....","....MM....","MMMMMMMMMM","MMMMMMMMMM","....MM....","....MM....","....MM....","....MM...."]
DIAM = ["....M....","...MMM...","..MMMMM..",".MMMMMMM.","MMMMMMMMM",".MMMMMMM.","..MMMMM..","...MMM...","....M...."]
STAR = ["...M...","..MMM..","MMMMMMM","MMMMMMM",".MMMMM.",".MMMMM.",".MM.MM."]
CELL = 20
# (art, couleur, taille finale en px PNG ; 2 px PNG = 1 px écran)
items = [(CAM,(235,25,170,255),(16,9)),(PLUS,(80,185,205,255),(10,10)),
         (DIAM,(16,128,225,255),(15,15)),(STAR,(0,249,134,255),(15,15))]
img = Image.new("RGBA",(CELL*len(items),CELL),(0,0,0,0))
for n,(art,col,(tw,th)) in enumerate(items):
    h,w=len(art),len(art[0])
    small = Image.new("RGBA",(w,h),(0,0,0,0))
    for y,row in enumerate(art):
        for x,c in enumerate(row):
            if c=="M": small.putpixel((x,y),col)
    if art is DIAM:
        big = Image.new("RGBA",(tw,th),(0,0,0,0))
        c = tw//2
        for y in range(th):
            hw = c - abs(y - th//2)
            for x in range(c-hw, c+hw+1):
                big.putpixel((x,y),col)
    else:
        big = small.resize((tw,th),Image.NEAREST)
    # centré sur la hauteur du texte (le texte monte à ~7 px au-dessus de la ligne de base, ascent=8)
    oy = max(0, 9 - th//2)
    img.paste(big,(n*CELL, oy))
img.save("src/main/resources/assets/rolenametag/textures/font/icons.png")
