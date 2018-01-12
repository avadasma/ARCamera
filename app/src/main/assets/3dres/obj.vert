attribute vec3 vPosition;
attribute vec2 vCoord;
uniform mat4 vMatrix;

varying vec2 textureCoordinate;

attribute vec3 vNormal;         //Normal vector
varying vec4 vDiffuse;          //The final intensity of the scattered light that is passed to the fragment shader


//Backscattered light intensity
vec4 pointLight(vec3 normal,vec3 lightLocation,vec4 lightDiffuse){
    //The transformed normal vector
    vec3 newTarget=normalize((vMatrix*vec4(normal+vPosition,1)).xyz-(vMatrix*vec4(vPosition,1)).xyz);
    //Surface point and light source of the direction vector
    vec3 vp=normalize(lightLocation-(vMatrix*vec4(vPosition,1)).xyz);
    return lightDiffuse*max(0.0,dot(newTarget,vp));
}

void main(){
    gl_Position = vMatrix*vec4(vPosition,1);
    textureCoordinate = vCoord;

   vec4 at=vec4(1.0,1.0,1.0,1.0);   //Light intensity
   vec3 pos=vec3(50.0,200.0,50.0);      //Light position
   vDiffuse=pointLight(vNormal,pos,at);
}