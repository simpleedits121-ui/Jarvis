from PIL import Image

source = "app/src/main/res/drawable/character_sheet.png"
output = "app/src/main/res/drawable/character_full_body.png"

image = Image.open(source).convert("RGBA")

# Crop the large character on the left.
# Keep transparency and avoid the bottom sprite reference panel.
character = image.crop((0, 0, 445, 1375))
character.save(output, optimize=True)

print("Created:", output)
print("Dimensions:", character.size)
print("Mode:", character.mode)
